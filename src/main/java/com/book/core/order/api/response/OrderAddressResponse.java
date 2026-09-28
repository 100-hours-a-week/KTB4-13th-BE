package com.book.core.order.api.response;

// @formatter:off
public record OrderAddressResponse(
        String postalCode,
        String address,
        String detailAddress) {}
// @formatter:on
