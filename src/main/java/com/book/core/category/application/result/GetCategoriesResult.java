package com.book.core.category.application.result;

import java.util.List;

public record GetCategoriesResult(List<GetCategoryItemResult> categories) {
    public static GetCategoriesResult of(final List<GetCategoryItemResult> categories) {
        return new GetCategoriesResult(categories);
    }
}
