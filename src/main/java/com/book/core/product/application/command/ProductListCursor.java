package com.book.core.product.application.command;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

// @formatter:off
public record ProductListCursor(Long productId, Long salesQuantity, Long reviewCount, BigDecimal reviewRate) {
    public boolean hasPopularityKeys() {
        return salesQuantity != null
                && salesQuantity >= 0
                && reviewCount != null
                && reviewCount >= 0
                && reviewRate != null
                && reviewRate.signum() >= 0
                && reviewRate.compareTo(BigDecimal.TEN) <= 0;
    }

    public String toPopularityToken() {
        final String value = String.join(
                ":",
                salesQuantity.toString(),
                reviewCount.toString(),
                reviewRate.stripTrailingZeros().toPlainString(),
                productId.toString());
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
// @formatter:on
