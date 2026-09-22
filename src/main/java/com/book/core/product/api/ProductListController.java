package com.book.core.product.api;

import com.book.common.response.ApiResponse;
import com.book.core.product.api.converter.ProductCommandConverter;
import com.book.core.product.api.converter.ProductResultConverter;
import com.book.core.product.api.response.ProductListResponse;
import com.book.core.product.api.spec.ProductListControllerSpec;
import com.book.core.product.application.service.ProductService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/items")
class ProductListController implements ProductListControllerSpec {
    private final ProductService productService;
    private final ProductCommandConverter commandConverter;
    private final ProductResultConverter resultConverter;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
            @Positive @RequestParam(value = "categoryId", required = false) final Long categoryId,
            @RequestParam(value = "sort", required = false) final String sort,
            @RequestParam(value = "cursor", required = false) final String cursor,
            @Positive @RequestParam(value = "limit", required = false) final Integer limit) {
        final var command = commandConverter.toGetProductsCommand(categoryId, sort, cursor, limit);
        final var result = productService.getProducts(command);
        final var response = resultConverter.toGetProductsResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
