package com.book.core.cart.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartAddRequest(
        @NotNull @Positive Long productId,
        @NotNull @Min(1) @Max(500) Integer quantity) {}
