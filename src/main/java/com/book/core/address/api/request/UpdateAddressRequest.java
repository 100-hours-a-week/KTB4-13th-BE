package com.book.core.address.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record UpdateAddressRequest(
        @NotBlank String label,
        @NotBlank String postalCode,
        @NotBlank String address,
        @JsonProperty(required = true) String detailAddress) {}
