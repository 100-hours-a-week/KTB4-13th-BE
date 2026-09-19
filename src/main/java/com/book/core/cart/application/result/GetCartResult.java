package com.book.core.cart.application.result;

import java.util.List;

public record GetCartResult(List<GetCartItemResult> items) {
    public static GetCartResult of(final List<GetCartItemResult> items) {
        return new GetCartResult(items);
    }

    public static GetCartResult empty() {
        return new GetCartResult(List.of());
    }
}
