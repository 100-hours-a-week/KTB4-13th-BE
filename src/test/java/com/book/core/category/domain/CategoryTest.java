package com.book.core.category.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {
    @Test
    void 생성된_카테고리는_활성_카테고리다() {
        final var category = new Category(7L, "소설", "도서/소설");

        assertThat(category.isActive()).isTrue();
    }

    @Test
    void 삭제한_카테고리는_비활성_카테고리다() {
        final var category = new Category(7L, "소설", "도서/소설");
        category.delete();

        assertThat(category.isActive()).isFalse();
    }

    @Test
    void 계층_경로를_그대로_보존한다() {
        final var category = new Category(7L, "소설", "도서/소설");

        assertThat(category.path()).isEqualTo("도서/소설");
    }
}
