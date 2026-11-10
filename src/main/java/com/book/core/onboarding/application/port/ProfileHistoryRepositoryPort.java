package com.book.core.onboarding.application.port;

import com.book.core.onboarding.application.result.OwnedBook;
import java.time.Instant;
import java.util.List;

public interface ProfileHistoryRepositoryPort {
    AiPersonalizationProfileHistory findHistory(final Long userId);

    List<OwnedBook> findOwnedBooks(final Long userId);

    List<Long> findChangedUserIds(final Instant from, final Instant before);
}
