package com.book.core.cart.api;

import com.book.common.response.ApiResponse;
import com.book.core.cart.api.converter.CartCommandConverter;
import com.book.core.cart.api.converter.CartResultConverter;
import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.response.CartResponse;
import com.book.core.cart.api.spec.CartControllerSpec;
import com.book.core.cart.application.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
class CartController implements CartControllerSpec {
    private final CartService cartService;
    private final CartCommandConverter commandConverter;
    private final CartResultConverter resultConverter;

    @Override
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> addCartItem(
            @Positive @RequestParam("userId") final Long userId, @Valid @RequestBody final AddCartItemRequest request) {
        final var command = commandConverter.toAddCartItemCommand(userId, request);
        cartService.addCartItem(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @Positive @RequestParam("userId") final Long userId,
            @Positive @PathVariable("cartItemId") final Long cartItemId) {
        final var command = commandConverter.toDeleteCartItemCommand(userId, cartItemId);
        cartService.deleteCartItem(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@Positive @RequestParam("userId") final Long userId) {
        final var command = commandConverter.toGetCartCommand(userId);
        final var cartResponse = resultConverter.toGetCartResponse(cartService.getCart(command));
        return ResponseEntity.ok(ApiResponse.ok(cartResponse));
    }
}
