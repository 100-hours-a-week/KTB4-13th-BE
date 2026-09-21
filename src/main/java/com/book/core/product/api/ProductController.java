package com.book.core.product.api;

import com.book.common.response.ApiResponse;
import com.book.core.product.api.converter.ProductCommandConverter;
import com.book.core.product.api.converter.ProductResultConverter;
import com.book.core.product.api.response.ProductDetailResponse;
import com.book.core.product.api.spec.ProductControllerSpec;
import com.book.core.product.application.service.ProductService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
class ProductController implements ProductControllerSpec {
    private final ProductService productService;
    private final ProductCommandConverter commandConverter;
    private final ProductResultConverter resultConverter;

    @Override
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(
            @Positive @PathVariable("productId") final Long productId) {
        final var command = commandConverter.toGetProductDetailCommand(productId);
        final var result = productService.getProductDetail(command);
        final var response = resultConverter.toGetProductDetailResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
