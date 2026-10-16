package com.book.core.cancel.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequest(@NotBlank @Size(max = 255) String orderKey) {
}
