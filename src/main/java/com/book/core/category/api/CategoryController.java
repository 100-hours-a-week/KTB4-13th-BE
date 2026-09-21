package com.book.core.category.api;

import com.book.common.response.ApiResponse;
import com.book.core.category.api.response.CategoryListResponse;
import com.book.core.category.api.spec.CategoryControllerSpec;
import com.book.core.category.application.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
class CategoryController implements CategoryControllerSpec {
    private final CategoryService categoryService;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<CategoryListResponse>> getCategories() {
        final var result = categoryService.getCategories();
        final var response = CategoryListResponse.from(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
