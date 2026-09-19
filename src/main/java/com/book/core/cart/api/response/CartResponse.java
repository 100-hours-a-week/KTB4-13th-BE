package com.book.core.cart.api.response;

import java.util.List;

public record CartResponse(List<CartItemResponse> items) {}
