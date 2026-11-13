package com.book.core.address.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
    @NotBlank @Size(max = 255) String label,
    @NotBlank @Size(max = 255) String postalCode,
    @NotBlank @Size(max = 255) String address,
    @JsonProperty(required = true) @Size(max = 255) String detailAddress) {
}
