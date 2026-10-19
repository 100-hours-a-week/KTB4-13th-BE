package com.book.core.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UserLibraryBookTest {
    private static final LocalDateTime ADDED_AT = LocalDateTime.of(2026, 10, 1, 10, 0);

    @ParameterizedTest
    @CsvSource(value = {"0, 1, 2026-10-01T10:00", "1, -1, 2026-10-01T10:00", "null, 1, 2026-10-01T10:00", "1, null, 2026-10-01T10:00",
        "1, 1, null"}, nullValues = "null")
    void 양수가_아닌_식별자나_추가_시각_없이는_생성하지_않는다(final Long userId, final Long bookId, final LocalDateTime addedAt) {
        assertThatThrownBy(() -> UserLibraryBook.add(userId, bookId, addedAt)).isInstanceOfSatisfying(CoreException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 삭제된_항목은_유효하지_않은_시각으로_복구하지_않고_상태를_유지한다() {
        final UserLibraryBook libraryBook = UserLibraryBook.add(1L, 2L, ADDED_AT);
        libraryBook.delete();

        assertThatThrownBy(() -> libraryBook.restore(null)).isInstanceOfSatisfying(CoreException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThat(libraryBook.isDeleted()).isTrue();
        assertThat(libraryBook.addedAt()).isEqualTo(ADDED_AT);
    }
}
