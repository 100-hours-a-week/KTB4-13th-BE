package com.book.core.cart.application.result;

import java.util.List;

public record GetCartResult(List<GetCartItemResult> items) {}
