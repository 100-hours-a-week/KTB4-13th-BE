package com.book.core.order.application.result;

import com.book.core.order.domain.OrderAddress;

// @formatter:off
public record OrderAddressResult(String postalCode, String address, String detailAddress) {
    public static OrderAddressResult from(final OrderAddress address) {
        return address == null ? null : new OrderAddressResult(address.postalCode(), address.address(), address.detailAddress());
    }
}
// @formatter:on
