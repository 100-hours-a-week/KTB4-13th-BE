package com.book.core.coupon.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReplayIssuanceDltRequest(@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> ids) {
}
