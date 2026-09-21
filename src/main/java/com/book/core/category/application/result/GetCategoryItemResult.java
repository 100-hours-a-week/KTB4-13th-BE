package com.book.core.category.application.result;

import com.book.core.category.domain.Category;

public record GetCategoryItemResult(Long id, String name, String path) {
    public static GetCategoryItemResult from(final Category category) {
        return new GetCategoryItemResult(category.id(), category.name(), category.path());
    }
}
