package com.book.core.onboarding.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.config.kafka.KafkaTopics;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.port.AiPersonalizationProfileHistory;
import com.book.core.onboarding.application.port.AiPersonalizationProfileMemory;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiPersonalizationProfileClientImplTest {
    private static final String SERVICE_TOKEN = "profile-service-token";
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String PROFILE_URL = BASE_URL + "/preferences/profile";

    private final RestClient.Builder builder =
        RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PersonalizationProfileIdempotencyKeyGenerator idempotencyKeyGenerator =
        new PersonalizationProfileIdempotencyKeyGenerator();
    private final GetAiBookIdentifiersUseCase identifiers = mock(GetAiBookIdentifiersUseCase.class);
    private final AiPersonalizationProfileExternalMapper mapper =
        new AiPersonalizationProfileExternalMapper(idempotencyKeyGenerator, identifiers);
    private final AiPersonalizationProfileClientImpl client = new AiPersonalizationProfileClientImpl(builder.build(), mapper);

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestMdcFilter.REQUEST_ID_MDC_KEY);
    }

    @Test
    void AI가_오류_상태를_응답하면_AI_서비스_호출_실패로_변환한다() {
        server.expect(requestTo(PROFILE_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.updateProfile(profileRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    @Test
    void 응답_시간_초과나_연결_실패는_AI_서비스_호출_실패로_변환한다() {
        server.expect(requestTo(PROFILE_URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.updateProfile(profileRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    @Test
    void PUT이_400으로_거부되면_기존처럼_재시도할_AI_서비스_호출_실패로_변환한다() {
        server.expect(requestTo(PROFILE_URL)).andExpect(method(HttpMethod.PUT)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.updateProfile(profileRequest())).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    @Test
    void DELETE가_400으로_거부되면_재시도하지_않을_요청_거부로_변환한다() {
        server.expect(requestTo(PROFILE_URL + "?user_id=42")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.deleteProfile(42L)).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_INVALID_REQUEST));
    }

    @Test
    void DELETE_preferences_profile에_인증_헤더와_회원_ID_쿼리를_보내고_이미_없는_프로필도_성공으로_처리한다() {
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "request-delete-789");
        server.expect(requestTo(PROFILE_URL + "?user_id=42")).andExpect(method(HttpMethod.DELETE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN)).andExpect(header("X-Request-Id", "request-delete-789"))
            .andRespond(withSuccess("""
                {"message":"profile_delete_success","data":{"deleted":false}}
                """, MediaType.APPLICATION_JSON));

        client.deleteProfile(42L, UUID.randomUUID());

        server.verify();
    }

    @Test
    void PUT_preferences_profile에_인증_헤더와_요청_본문을_보낸다() {
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "request-put-456");
        final AiPersonalizationProfileRequest request = profileRequest();
        when(identifiers.findIsbns(List.of(100L, 300L))).thenReturn(List.of("9788900000011", "9788900000042"));
        server.expect(requestTo(PROFILE_URL)).andExpect(method(HttpMethod.PUT))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SERVICE_TOKEN)).andExpect(header("X-Request-Id", "request-put-456"))
            .andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.idempotency_key").value(idempotencyKeyGenerator.generate(request)))
            .andExpect(jsonPath("$.onboarding.reading_times[0]").value("잠들기 전"))
            .andExpect(jsonPath("$.onboarding.criteria[0]").value("베스트셀러")).andExpect(jsonPath("$.onboarding.categories[0]").value("소설"))
            .andExpect(jsonPath("$.onboarding.tags[0]").value("SF"))
            .andExpect(jsonPath("$.onboarding.liked_isbns[1]").value("9788900000042")).andExpect(jsonPath("$.memories").isEmpty())
            .andRespond(withSuccess());

        client.updateProfile(request, UUID.randomUUID());

        server.verify();
    }

    @Test
    void 취향_기억과_이력을_AI_계약의_칸으로_보내고_저장_시각은_마이크로초까지_싣는다() {
        final AiPersonalizationProfileRequest request =
            new AiPersonalizationProfileRequest(42L, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new AiPersonalizationProfileMemory("author", "김영하의 문장을 좋아함", 2, List.of(), "김영하",
                    LocalDateTime.of(2026, 9, 4, 0, 12, 30, 123_456_000), List.of(0.02f), 1)),
                new AiPersonalizationProfileHistory(List.of("9788900000011"), List.of("9788900000042"),
                    List.of(new AiPersonalizationProfileHistory.Review("9788900000011", new BigDecimal("4.5"), "울컥했어요"))));
        server.expect(requestTo(PROFILE_URL)).andExpect(method(HttpMethod.PUT)).andExpect(jsonPath("$.memories[0].type").value("author"))
            .andExpect(jsonPath("$.memories[0].strength").value(2)).andExpect(jsonPath("$.memories[0].applies_to").isEmpty())
            .andExpect(jsonPath("$.memories[0].keyword").value("김영하"))
            .andExpect(jsonPath("$.memories[0].created_at").value("2026-09-04T00:12:30.123456"))
            .andExpect(jsonPath("$.memories[0].dim").value(1)).andExpect(jsonPath("$.history.purchased_isbns[0]").value("9788900000011"))
            .andExpect(jsonPath("$.history.library_isbns[0]").value("9788900000042"))
            .andExpect(jsonPath("$.history.reviews[0].rating").value(4.5))
            .andExpect(jsonPath("$.history.reviews[0].content").value("울컥했어요")).andRespond(withSuccess());

        client.updateProfile(request);

        server.verify();
    }

    @Test
    void HTTP_본문과_Kafka_프로필_본문이_일치한다() throws Exception {
        final var request = profileRequest();
        when(identifiers.findIsbns(List.of(100L, 300L))).thenReturn(List.of("9788900000011", "9788900000042"));
        final ObjectMapper json = new ObjectMapper();
        final String[] httpBody = new String[1];
        server.expect(requestTo(PROFILE_URL)).andExpect(req -> httpBody[0] = ((MockClientHttpRequest) req).getBodyAsString())
            .andRespond(withSuccess());
        client.updateProfile(request);
        final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(eq(KafkaTopics.AI_PROFILE), eq("42"), any(String.class))).thenReturn(CompletableFuture.completedFuture(null));
        new AiPersonalizationProfileKafkaClient(mapper, kafka, json).updateProfile(request, UUID.randomUUID());
        final ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(KafkaTopics.AI_PROFILE), eq("42"), sent.capture());
        assertThat(json.readTree(sent.getValue()).path("profile")).isEqualTo(json.readTree(httpBody[0]));
        server.verify();
    }

    private static AiPersonalizationProfileRequest profileRequest() {
        return new AiPersonalizationProfileRequest(42L, List.of("잠들기 전"), List.of("베스트셀러"), List.of("소설"), List.of("SF"),
            List.of(100L, 300L));
    }
}
