package com.book.core.onboarding.infrastructure.client.ai;

import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import com.book.core.onboarding.application.usecase.RequestProfileSyncUseCase;
import com.book.core.user.application.usecase.CheckWithdrawalConfirmedUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.profile-sync.ai-kafka-enabled", havingValue = "true")
class AiPersonalizationProfileRoutingClient implements AiPersonalizationProfileClient {
    private final AiPersonalizationProfileClientImpl httpClient;
    private final AiPersonalizationProfileKafkaClient kafkaClient;
    private final CheckWithdrawalConfirmedUseCase withdrawalConfirmed;
    private final RequestProfileSyncUseCase requestSync;

    @Override
    public void updateProfile(final AiPersonalizationProfileRequest request) {
        httpClient.updateProfile(request);
    }

    @Override
    public void updateProfile(final AiPersonalizationProfileRequest request, final UUID revision) {
        kafkaClient.updateProfile(request, revision);
        if (withdrawalConfirmed.execute(request.userId())) {
            requestSync.execute(request.userId());
        }
    }

    @Override
    public void deleteProfile(final Long userId) {
        httpClient.deleteProfile(userId);
    }

    @Override
    public void deleteProfile(final Long userId, final UUID revision) {
        kafkaClient.deleteProfile(userId, revision);
    }
}
