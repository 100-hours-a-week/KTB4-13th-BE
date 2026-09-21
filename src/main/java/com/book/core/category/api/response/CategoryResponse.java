package com.book.core.category.api.response;

import com.book.core.category.application.result.GetCategoryItemResult;

public record CategoryResponse(Long id, String name, String path) {
    public static CategoryResponse from(final GetCategoryItemResult result) {
        return new CategoryResponse(result.id(), result.name(), result.path());
    }
}
