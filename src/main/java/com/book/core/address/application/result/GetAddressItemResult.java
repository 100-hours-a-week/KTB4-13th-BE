package com.book.core.address.application.result;

public record GetAddressItemResult(
        Long addressId,
        String addressLabel,
        String addressPostalCode,
        String address,
        String detailAddress,
        boolean defaultAddress) {}
