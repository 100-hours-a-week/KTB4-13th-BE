package com.book.core.category.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CategoryTest {
    @Test
    void 삭제일이_없으면_활성_카테고리다() {
        final var category = new Category(7L, "소설", "도서/소설", null);

        assertThat(category.isActive()).isTrue();
    }

    @Test
    void 삭제일이_있으면_비활성_카테고리다() {
        final var category = new Category(7L, "소설", "도서/소설", LocalDateTime.parse("2026-01-01T00:00:00"));

        assertThat(category.isActive()).isFalse();
    }

    @Test
    void 계층_경로를_그대로_보존한다() {
        final var category = new Category(7L, "소설", "도서/소설", null);

        assertThat(category.path()).isEqualTo("도서/소설");
    }
}
