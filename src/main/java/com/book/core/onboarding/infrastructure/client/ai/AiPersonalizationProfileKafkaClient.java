package com.book.core.onboarding.infrastructure.client.ai;

import com.book.common.config.kafka.KafkaTopics;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.profile-sync.ai-kafka-enabled", havingValue = "true")
class AiPersonalizationProfileKafkaClient {
    /** AI 소비자가 받는 메시지 값의 최대 크기(PROFILE_KAFKA_MAX_MESSAGE_BYTES)와 같습니다. */
    static final int MAX_MESSAGE_BYTES = 8 * 1024 * 1024;
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
        final String message;
        try {
            message = json.writeValueAsString(event);
        } catch (final RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
        if (message.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
            throw new CoreException(ErrorCode.AI_PROFILE_MESSAGE_TOO_LARGE);
        }
        try {
            kafka.send(KafkaTopics.AI_PROFILE, userId.toString(), message).get(12, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        } catch (final ExecutionException | RuntimeException exception) {
            final ErrorCode errorCode = tooLarge(exception) ? ErrorCode.AI_PROFILE_MESSAGE_TOO_LARGE : ErrorCode.AI_SERVICE_FAILURE;
            throw new CoreException(errorCode, exception);
        } catch (final TimeoutException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

    /** Producer 한도 초과는 send에서 바로, 브로커·토픽 한도 초과는 발행 결과로 RecordTooLargeException이 원인에 담겨 옵니다. */
    private static boolean tooLarge(final Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof RecordTooLargeException) {
                return true;
            }
        }
        return false;
    }
}
