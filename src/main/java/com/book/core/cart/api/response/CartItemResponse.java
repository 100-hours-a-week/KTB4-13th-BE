package com.book.core.cart.api.response;

public record CartItemResponse(Long cartItemId, Long productId, Integer quantity) {}
