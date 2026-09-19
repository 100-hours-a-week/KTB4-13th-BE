package com.book.core.cart.application.result;

import java.util.List;

public record CartQueryResult(List<CartItemQueryResult> items) {}
