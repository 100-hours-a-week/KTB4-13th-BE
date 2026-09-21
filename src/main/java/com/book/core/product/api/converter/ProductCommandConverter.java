package com.book.core.product.api.converter;

import com.book.core.product.application.command.GetProductDetailCommand;
import org.springframework.stereotype.Component;

@Component
public class ProductCommandConverter {
    public GetProductDetailCommand toGetProductDetailCommand(final Long productId) {
        return new GetProductDetailCommand(productId);
    }
}
