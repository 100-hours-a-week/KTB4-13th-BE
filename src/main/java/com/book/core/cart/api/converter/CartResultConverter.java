package com.book.core.cart.api.converter;

import com.book.core.cart.api.response.CartItemResponse;
import com.book.core.cart.api.response.CartResponse;
import com.book.core.cart.application.result.GetCartResult;
import org.springframework.stereotype.Component;

@Component
public class CartResultConverter {
    public CartResponse toGetCartResponse(final GetCartResult result) {
        return new CartResponse(result.items().stream()
                .map(item -> new CartItemResponse(item.cartItemId(), item.productId(), item.quantity()))
                .toList());
    }
}
