package com.book.core.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.category.application.result.GetCategoriesResult;
import com.book.core.category.application.usecase.GetCategoriesUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {
    private final GetCategoriesUseCase getCategoriesUseCase = mock(GetCategoriesUseCase.class);
    private final CategoryService categoryService = new CategoryService(getCategoriesUseCase);

    @Test
    void 카테고리_목록_조회를_UseCase에_위임한다() {
        final var result = GetCategoriesResult.of(List.of());
        when(getCategoriesUseCase.execute()).thenReturn(result);

        assertThat(categoryService.getCategories()).isSameAs(result);

        verify(getCategoriesUseCase).execute();
    }
}
