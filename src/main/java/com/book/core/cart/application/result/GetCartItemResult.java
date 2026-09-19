package com.book.core.cart.application.result;

public record GetCartItemResult(Long cartItemId, Long productId, Integer quantity) {}
