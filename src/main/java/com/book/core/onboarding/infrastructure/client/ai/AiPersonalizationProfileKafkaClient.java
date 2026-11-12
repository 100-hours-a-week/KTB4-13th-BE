package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.config.kafka.KafkaTopics;
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
    private final AiPersonalizationProfileExternalMapper mapper;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    void updateProfile(final AiPersonalizationProfileRequest request, final UUID revision) {
        final AiProfileRequest profile;
        try {
            profile = mapper.toProfileRequest(request);
        } catch (final RuntimeException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
        publish(request.userId(), new AiPersonalizationProfileKafkaEvent(2, "UPSERT", request.userId(), revision, profile));
    }

    void deleteProfile(final Long userId, final UUID revision) {
        publish(userId, new AiPersonalizationProfileKafkaEvent(2, "DELETE", userId, revision, null));
    }

    private void publish(final Long userId, final AiPersonalizationProfileKafkaEvent event) {
        try {
            final String message = json.writeValueAsString(event);
            kafka.send(KafkaTopics.AI_PROFILE, userId.toString(), message).get(12, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        } catch (final ExecutionException | TimeoutException | RuntimeException
            | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

}
