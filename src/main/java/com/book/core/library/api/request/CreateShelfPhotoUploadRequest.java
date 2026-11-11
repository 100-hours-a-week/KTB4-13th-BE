package com.book.core.library.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateShelfPhotoUploadRequest(@NotBlank String contentType, @NotNull @Positive Long contentLength) {
}
