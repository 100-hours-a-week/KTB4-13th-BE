package com.book.core.onboarding.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class AiPersonalizationProfileKafkaClientTest {
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final GetAiBookIdentifiersUseCase identifiers = mock(GetAiBookIdentifiersUseCase.class);
    private final AiPersonalizationProfileExternalMapper mapper =
        new AiPersonalizationProfileExternalMapper(new PersonalizationProfileIdempotencyKeyGenerator(), identifiers);
    private final ObjectMapper json = new ObjectMapper();
    private final AiPersonalizationProfileKafkaClient client = new AiPersonalizationProfileKafkaClient(mapper, kafka, json);
    private final AiPersonalizationProfileRequest request =
        new AiPersonalizationProfileRequest(42L, List.of("잠들기 전"), List.of(), List.of("소설"), List.of(), List.of(100L));

    @Test
    void 사용자_key와_기존_HTTP_변환_결과를_발행한다() throws Exception {
        final UUID revision = UUID.randomUUID();
        when(identifiers.findIsbns(List.of(100L))).thenReturn(List.of("9788900000011"));
        when(kafka.send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        client.updateProfile(request, revision);

        final ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), value.capture());
        final var event = json.readTree(value.getValue());
        assertThat(event.get("schema_version").asInt()).isEqualTo(1);
        assertThat(event.get("job_revision").asText()).isEqualTo(revision.toString());
        assertThat(event.get("profile")).isEqualTo(json.readTree(json.writeValueAsString(mapper.toProfileRequest(request))));
        assertThat(event.at("/profile/onboarding/liked_isbns/0").asText()).isEqualTo("9788900000011");
    }

    @Test
    void 발행_실패는_작업_재시도에_사용할_예외로_전파한다() {
        final CompletableFuture<SendResult<String, String>> result = new CompletableFuture<>();
        result.completeExceptionally(new RuntimeException("broker unavailable"));
        when(kafka.send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), any(String.class))).thenReturn(result);

        assertThatThrownBy(() -> client.updateProfile(request, UUID.randomUUID())).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    @Test
    void AI_수신_한도인_8MiB까지는_발행하고_넘으면_발행하지_않고_크기_초과로_알린다() throws Exception {
        final UUID revision = UUID.randomUUID();
        final int baseBytes =
            json.writeValueAsBytes(new AiPersonalizationProfileKafkaEvent(1, revision, mapper.toProfileRequest(requestWithTag("")))).length;
        final String limitTag = "x".repeat(AiPersonalizationProfileKafkaClient.MAX_MESSAGE_BYTES - baseBytes);
        when(kafka.send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), any(String.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        client.updateProfile(requestWithTag(limitTag), revision);

        final ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), value.capture());
        assertThat(value.getValue().getBytes(StandardCharsets.UTF_8)).hasSize(AiPersonalizationProfileKafkaClient.MAX_MESSAGE_BYTES);
        assertThatThrownBy(() -> client.updateProfile(requestWithTag(limitTag + "x"), revision)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_PROFILE_MESSAGE_TOO_LARGE));
        verify(kafka, times(1)).send(any(), any(), any());
    }

    @Test
    void Producer나_브로커가_크기_한도로_거부하면_크기_초과로_알린다() {
        final CompletableFuture<SendResult<String, String>> result = new CompletableFuture<>();
        result.completeExceptionally(new KafkaProducerException(null, "send failed", new RecordTooLargeException("too large")));
        when(kafka.send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), any(String.class)))
            .thenThrow(new KafkaException("Send failed", new RecordTooLargeException("too large"))).thenReturn(result);

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> client.updateProfile(request, UUID.randomUUID())).isInstanceOfSatisfying(CoreException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_PROFILE_MESSAGE_TOO_LARGE));
        }
    }

    @Test
    void 응답_시간_초과는_재시도되고_동일_revision이_다시_발행될_수_있다() throws Exception {
        final UUID revision = UUID.randomUUID();
        final CompletableFuture<SendResult<String, String>> uncertain = mock(CompletableFuture.class);
        when(uncertain.get(12, TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        when(kafka.send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), any(String.class))).thenReturn(uncertain,
            CompletableFuture.completedFuture(null));

        assertThatThrownBy(() -> client.updateProfile(request, revision)).isInstanceOf(CoreException.class);
        client.updateProfile(request, revision);

        final ArgumentCaptor<String> values = ArgumentCaptor.forClass(String.class);
        verify(kafka, times(2)).send(eq(AiPersonalizationProfileKafkaClient.TOPIC), eq("42"), values.capture());
        assertThat(json.readTree(values.getAllValues().get(0)).path("job_revision").asText()).isEqualTo(revision.toString());
        assertThat(json.readTree(values.getAllValues().get(1)).path("job_revision").asText()).isEqualTo(revision.toString());
    }

    @Test
    void 라우터는_일반_요청을_HTTP로_작업_요청은_Kafka로_전달한다() {
        final var http = mock(AiPersonalizationProfileClientImpl.class);
        final var publisher = mock(AiPersonalizationProfileKafkaClient.class);
        final var router = new AiPersonalizationProfileRoutingClient(http, publisher);
        final var revision = UUID.randomUUID();

        router.updateProfile(request);
        router.updateProfile(request, revision);
        router.deleteProfile(42L);

        verify(http).updateProfile(request);
        verify(publisher).updateProfile(request, revision);
        verify(http).deleteProfile(42L);
        verifyNoMoreInteractions(publisher);
    }

    private static AiPersonalizationProfileRequest requestWithTag(final String tag) {
        return new AiPersonalizationProfileRequest(42L, List.of(), List.of(), List.of(), List.of(tag), List.of());
    }

}
