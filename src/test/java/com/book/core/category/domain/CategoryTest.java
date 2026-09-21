package com.book.core.category.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CategoryTest {
    @Test
    void ACTIVE_상태면_활성_카테고리다() {
        final var category = new Category(7L, "소설", "도서/소설");

        assertThat(category.isActive()).isTrue();
    }

    @Test
    void DELETED_상태면_비활성_카테고리다() {
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
