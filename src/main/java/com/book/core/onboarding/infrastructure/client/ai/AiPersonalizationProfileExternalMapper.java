package com.book.core.onboarding.infrastructure.client.ai;

import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AiPersonalizationProfileExternalMapper {
    private final PersonalizationProfileIdempotencyKeyGenerator idempotencyKeyGenerator;
    private final GetAiBookIdentifiersUseCase identifiers;

    AiProfileRequest toProfileRequest(final AiPersonalizationProfileRequest request) {
        return AiProfileRequest.from(request, idempotencyKeyGenerator.generate(request), identifiers.findIsbns(request.likedBookIds()));
    }
}
