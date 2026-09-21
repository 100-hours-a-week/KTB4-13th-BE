package com.book.core.address.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterAddressRequest(
        @NotBlank String label,
        @NotBlank String postalCode,
        @NotBlank String address,
        String detailAddress,
        @NotNull Boolean isDefault) {}
