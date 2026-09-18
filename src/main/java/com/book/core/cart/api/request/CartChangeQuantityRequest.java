package com.book.core.cart.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record CartChangeQuantityRequest(@NotNull @Max(500) Integer quantity) {}
