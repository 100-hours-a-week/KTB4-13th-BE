package com.book.core.product.api.converter;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.command.ProductListCursor;
import com.book.core.product.application.command.ProductListSort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class ProductCommandConverter {
    public GetProductDetailCommand toGetProductDetailCommand(final Long productId) {
        return GetProductDetailCommand.of(productId);
    }

    public GetProductsCommand toGetProductsCommand(final Long categoryId, final String sort, final String cursor, final Integer limit) {
        final ProductListSort parsedSort = parseSort(sort);
        return new GetProductsCommand(categoryId, parsedSort, parseCursor(parsedSort, cursor), limit);
    }

    private ProductListSort parseSort(final String sort) {
        if (sort == null || "createdAt".equals(sort)) {
            return ProductListSort.CREATED_AT;
        }
        if ("POPULARITY".equals(sort)) {
            return ProductListSort.POPULARITY;
        }
        throw new CoreException(ErrorCode.INVALID_REQUEST);
    }

    private ProductListCursor parseCursor(final ProductListSort sort, final String cursor) {
        if (cursor == null) {
            return null;
        }
        try {
            if (sort == ProductListSort.CREATED_AT) {
                return new ProductListCursor(Long.parseLong(cursor), null, null, null);
            }
            final String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split(":", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException();
            }
            return new ProductListCursor(Long.parseLong(parts[3]), Long.parseLong(parts[0]), Long.parseLong(parts[1]),
                new BigDecimal(parts[2]));
        } catch (IllegalArgumentException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
