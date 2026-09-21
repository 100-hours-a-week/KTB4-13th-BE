package com.book.core.category.api.response;

import com.book.core.category.application.result.GetCategoriesResult;
import java.util.List;

public record CategoryListResponse(List<CategoryResponse> categories) {
    public static CategoryListResponse from(final GetCategoriesResult result) {
        final var categories =
                result.categories().stream().map(CategoryResponse::from).toList();
        return new CategoryListResponse(categories);
    }
}
