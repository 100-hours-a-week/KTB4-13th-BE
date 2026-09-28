package com.book.core.product.application.command;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// @formatter:off
public record ProductListCursor(Long productId, Long salesQuantity) {
    public boolean hasPopularityKeys() {
        return salesQuantity != null
                && salesQuantity >= 0;
    }

    public String toPopularityToken() {
        final String value = String.join(
                ":",
                salesQuantity.toString(),
                productId.toString());
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
// @formatter:on
