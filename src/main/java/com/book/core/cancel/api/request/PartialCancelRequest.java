package com.book.core.cancel.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PartialCancelRequest(@NotBlank @Size(max = 255) String orderKey, @NotNull @Positive Long orderItemId,
    @NotNull @Positive Long quantity) {
}
