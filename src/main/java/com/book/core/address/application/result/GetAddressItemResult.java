package com.book.core.address.application.result;

import com.book.core.address.domain.Address;

public record GetAddressItemResult(
        Long addressId,
        String addressLabel,
        String addressPostalCode,
        String address,
        String detailAddress,
        boolean defaultAddress) {
    public static GetAddressItemResult from(final Address address) {
        return new GetAddressItemResult(
                address.id(),
                address.label(),
                address.postalCode(),
                address.address(),
                address.detailAddress(),
                address.isDefaultAddress());
    }
}
