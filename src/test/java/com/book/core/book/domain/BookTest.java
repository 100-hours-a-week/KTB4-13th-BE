package com.book.core.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookTest {
    @Test
    void 도서_상세_속성을_보존한다() {
        final var publishedAt = LocalDate.of(2026, 1, 1);
        final var book = new Book(10L, "9781234567890", "1234567890", "도서명", "작가", "설명", "출판사", "소설", publishedAt, "cover.jpg");

        assertThat(book.id()).isEqualTo(10L);
        assertThat(book.title()).isEqualTo("도서명");
        assertThat(book.author()).isEqualTo("작가");
        assertThat(book.publishedAt()).isEqualTo(publishedAt);
    }

    @Test
    void 삭제된_도서는_활성_도서가_아니다() {
        final var book = new Book(10L, null, null, "도서명", "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);

        book.delete();

        assertThat(book.isActive()).isFalse();
        assertThat(book.deletedAt()).isNotNull();

        book.active();

        assertThat(book.isActive()).isTrue();
        assertThat(book.deletedAt()).isNull();
    }

    @Test
    void 신간_ISBN이_유효하지_않으면_공통_오류를_반환한다() {
        assertThatThrownBy(() -> Book.registerNewBook("invalid", "도서명", "저자", null, "출판사", "800", LocalDate.of(2026, 1, 1), null))
            .isInstanceOfSatisfying(CoreException.class,
                (final var error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 신간_등록_필수값이_없으면_공통_오류를_반환한다() {
        assertThatThrownBy(() -> Book.registerNewBook("9788983921987", "도서명", " ", null, "출판사", "800", LocalDate.of(2026, 1, 1), null))
            .isInstanceOfSatisfying(CoreException.class,
                (final var error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

}
