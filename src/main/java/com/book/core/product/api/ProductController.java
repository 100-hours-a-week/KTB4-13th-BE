package com.book.core.product.api;

import com.book.common.response.ApiResponse;
import com.book.core.product.api.converter.ProductCommandConverter;
import com.book.core.product.api.converter.ProductResultConverter;
import com.book.core.product.api.response.ProductDetailResponse;
import com.book.core.product.api.response.ProductListResponse;
import com.book.core.product.api.spec.ProductControllerSpec;
import com.book.core.product.application.service.ProductService;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
class ProductController implements ProductControllerSpec {
    private final ProductService productService;
    private final ProductCommandConverter commandConverter;
    private final ProductResultConverter resultConverter;

    @Override
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@Positive @PathVariable("productId") final Long productId) {
        final var command = commandConverter.toGetProductDetailCommand(productId);
        final var result = productService.getProductDetail(command);
        final var response = resultConverter.toGetProductDetailResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<ProductListResponse>> getProducts(
        @Positive @RequestParam(value = "categoryId", required = false) final Long categoryId,
        @RequestParam(value = "sort", required = false) final String sort,
        @RequestParam(value = "cursor", required = false) final String cursor,
        @Positive @RequestParam(value = "limit", required = false) final Integer limit,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @RequestParam(value = "publishedFrom", required = false) final LocalDate publishedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(value = "publishedTo", required = false) final LocalDate publishedTo) {
        final var command = commandConverter.toGetProductsCommand(categoryId, publishedFrom, publishedTo, sort, cursor, limit);
        final var result = productService.getProducts(command);
        final var response = resultConverter.toGetProductsResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
