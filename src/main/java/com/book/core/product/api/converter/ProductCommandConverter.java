package com.book.core.product.api.converter;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.GetProductsCommand;
import org.springframework.stereotype.Component;

@Component
public class ProductCommandConverter {
    public GetProductDetailCommand toGetProductDetailCommand(final Long productId) {
        return new GetProductDetailCommand(productId);
    }

    public GetProductsCommand toGetProductsCommand(
            final Long categoryId, final String sort, final String cursor, final Integer limit) {
        return new GetProductsCommand(categoryId, sort, parseCursor(cursor), limit);
    }

    private Long parseCursor(final String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
