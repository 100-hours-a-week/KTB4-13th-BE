package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.profile-sync.ai-kafka-enabled", havingValue = "true")
class AiPersonalizationProfileKafkaClient {
    static final String TOPIC = "book.ai-profile.v1";
    private final AiPersonalizationProfileExternalMapper mapper;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    void updateProfile(final AiPersonalizationProfileRequest request, final UUID revision) {
        try {
            final String message =
                json.writeValueAsString(new AiPersonalizationProfileKafkaEvent(1, revision, mapper.toProfileRequest(request)));
            kafka.send(TOPIC, request.userId().toString(), message).get(12, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        } catch (final ExecutionException | TimeoutException | RuntimeException
            | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

}
