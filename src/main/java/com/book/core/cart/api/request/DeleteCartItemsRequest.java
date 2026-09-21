package com.book.core.cart.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record DeleteCartItemsRequest(@NotEmpty List<@NotNull @Positive Long> cartItemIds) {}
