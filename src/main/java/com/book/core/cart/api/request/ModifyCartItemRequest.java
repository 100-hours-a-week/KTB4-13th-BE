package com.book.core.cart.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ModifyCartItemRequest(@NotNull @Min(1)@Max(500)Integer quantity){}
