package com.book.core.library.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ScanShelfPhotoRequest(@NotBlank @Size(max = 200) String photoKey) {
}
