package com.book.core.onboarding.application.port;

public interface AiPersonalizationProfileClient {
    void createProfile(final AiPersonalizationProfileRequest request);

    void updateProfile(final AiPersonalizationProfileRequest request);
}
