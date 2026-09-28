package com.book.core.onboarding.application.port;

public interface PersonalizationProfileClient {
    PersonalizationProfileResult createProfile(final PersonalizationProfileRequest request);
}
