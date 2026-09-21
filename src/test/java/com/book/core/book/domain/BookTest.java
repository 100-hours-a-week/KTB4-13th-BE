package com.book.core.book.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class BookTest {
    @Test
    void 도서_상세_속성을_보존한다() {
        final var publishedAt = LocalDate.of(2026, 1, 1);
        final var book =
                new Book(10L, "9781234567890", "1234567890", "도서명", "작가", "설명", "출판사", "소설", publishedAt, "cover.jpg");

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
    }
}
