package com.book.core.onboarding.application.port;

import java.util.UUID;

public interface AiPersonalizationProfileClient {
    void updateProfile(final AiPersonalizationProfileRequest request);

    default void updateProfile(final AiPersonalizationProfileRequest request, final UUID revision) {
        updateProfile(request);
    }

    /** 프로필이 이미 없어도 성공하는 멱등 요청입니다. */
    void deleteProfile(final Long userId);

    default void deleteProfile(final Long userId, final UUID revision) {
        deleteProfile(userId);
    }
}
