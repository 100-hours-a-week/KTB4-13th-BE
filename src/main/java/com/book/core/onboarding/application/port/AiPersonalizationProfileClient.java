package com.book.core.onboarding.application.port;

import java.util.UUID;

public interface AiPersonalizationProfileClient {
    void updateProfile(final AiPersonalizationProfileRequest request);

    default void updateProfile(final AiPersonalizationProfileRequest request, final UUID revision) {
        updateProfile(request);
    }
}
