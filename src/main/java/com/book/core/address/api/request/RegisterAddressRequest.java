package com.book.core.address.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterAddressRequest(
    @NotBlank @Size(max = 255) String label,
    @NotBlank @Size(max = 255) String postalCode,
    @NotBlank @Size(max = 255) String address,
    @Size(max = 255) String detailAddress,
    @NotNull Boolean isDefault) {
}
