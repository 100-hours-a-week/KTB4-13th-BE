package com.book.core.cart.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CartDeleteRequest(@NotEmpty @Size(max = 30) List<@NotNull @Positive Long> cartItemIds) {}
