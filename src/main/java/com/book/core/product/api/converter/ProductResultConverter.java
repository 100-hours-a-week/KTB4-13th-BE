package com.book.core.product.api.converter;

import com.book.core.product.api.response.ProductDetailResponse;
import com.book.core.product.application.result.GetProductDetailResult;
import org.springframework.stereotype.Component;

@Component
public class ProductResultConverter {
    public ProductDetailResponse toGetProductDetailResponse(final GetProductDetailResult result) {
        return ProductDetailResponse.from(result);
    }
}
