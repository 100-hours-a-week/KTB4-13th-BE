package com.book.core.address.api.response;

import com.book.core.address.application.result.ListAddressItemResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AddressResponse(
        @JsonProperty("addressid") Long addressId,
        String addressLabel,
        String addressPostalCode,
        String address,
        String detailAddress,
        boolean isDefault) {
    public static AddressResponse from(final ListAddressItemResult result) {
        return new AddressResponse(
                result.addressId(),
                result.addressLabel(),
                result.addressPostalCode(),
                result.address(),
                result.detailAddress(),
                result.defaultAddress());
    }
}
