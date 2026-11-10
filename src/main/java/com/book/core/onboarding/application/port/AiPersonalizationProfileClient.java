package com.book.core.onboarding.application.port;

public interface AiPersonalizationProfileClient {
    void updateProfile(final AiPersonalizationProfileRequest request);

    /** 프로필이 이미 없어도 성공으로 처리되는 멱등 요청입니다. */
    void deleteProfile(final Long userId);
}
