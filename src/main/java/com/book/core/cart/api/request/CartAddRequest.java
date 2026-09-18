package com.book.core.cart.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartAddRequest(
        @NotNull @Positive Long productId, @Min(1) @Max(500) Integer quantity) {
    public int quantityOrDefault() {
        if (quantity == null) {
            return 1;
        }
        return quantity;
    }
}
