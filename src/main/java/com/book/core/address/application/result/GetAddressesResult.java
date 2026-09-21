package com.book.core.address.application.result;

import java.util.List;

public record GetAddressesResult(List<GetAddressItemResult> addresses) {
    public static GetAddressesResult of(final List<GetAddressItemResult> addresses) {
        return new GetAddressesResult(addresses);
    }
}
