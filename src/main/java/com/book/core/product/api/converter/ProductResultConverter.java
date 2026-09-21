package com.book.core.product.api.converter;

import com.book.core.product.api.response.ProductDetailResponse;
import com.book.core.product.api.response.ProductListResponse;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.result.GetProductsResult;
import org.springframework.stereotype.Component;

@Component
public class ProductResultConverter {
    public ProductDetailResponse toGetProductDetailResponse(final GetProductDetailResult result) {
        return ProductDetailResponse.from(result);
    }

    public ProductListResponse toGetProductsResponse(final GetProductsResult result) {
        return ProductListResponse.from(result);
    }
}
