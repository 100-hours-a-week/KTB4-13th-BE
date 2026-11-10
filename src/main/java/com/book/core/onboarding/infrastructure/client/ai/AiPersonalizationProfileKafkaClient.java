package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.profile-sync.ai-kafka-enabled", havingValue = "true")
class AiPersonalizationProfileKafkaClient implements AiPersonalizationProfileClient {
    static final String TOPIC = "book.ai-profile.v1";
    private final AiPersonalizationProfileClientImpl httpClient;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    @Override
    public void updateProfile(final AiPersonalizationProfileRequest request) {
        httpClient.updateProfile(request);
    }

    @Override
    public void updateProfile(final AiPersonalizationProfileRequest request, final UUID revision) {
        try {
            final String message = json.writeValueAsString(new ProfileEvent(1, revision, httpClient.toProfileRequest(request)));
            kafka.send(TOPIC, request.userId().toString(), message).get(12, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        } catch (final ExecutionException | TimeoutException | RuntimeException
            | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

    private record ProfileEvent(@JsonProperty("schema_version") int schemaVersion, @JsonProperty("job_revision") UUID jobRevision,
        @JsonProperty("profile") AiProfileRequest profile) {
    }
}
