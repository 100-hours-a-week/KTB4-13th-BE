package com.book.core.cart.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull @Positive Long productId,

        @NotNull
        @Min(value = 1, message = "장바구니 상품 수량은 1 이상이어야 합니다.")
        @Max(value = 500, message = "장바구니 상품 수량은 500 이하여야 합니다.")
        Integer quantity) {}
