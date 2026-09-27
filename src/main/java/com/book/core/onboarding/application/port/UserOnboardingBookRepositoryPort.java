package com.book.core.onboarding.application.port;

import com.book.core.onboarding.domain.UserOnboardingBook;
import java.util.List;

public interface UserOnboardingBookRepositoryPort {
    List<UserOnboardingBook> findByUserId(final Long userId);

    void deleteByUserId(final Long userId);

    List<UserOnboardingBook> saveAll(final List<UserOnboardingBook> books);
}
