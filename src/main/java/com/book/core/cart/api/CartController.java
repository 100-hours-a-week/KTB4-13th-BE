package com.book.core.cart.api;

import com.book.common.response.ApiResponse;
import com.book.core.cart.api.converter.CartCommandConverter;
import com.book.core.cart.api.converter.CartResultConverter;
import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.response.CartResponse;
import com.book.core.cart.api.spec.CartControllerSpec;
import com.book.core.cart.application.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<ApiResponse<Void>> addCartItem(@AuthenticationPrincipal final Jwt jwt,
        @Valid @RequestBody final AddCartItemRequest request) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toAddCartItemCommand(userId, request);
        cartService.addCartItem(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal final Jwt jwt) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toGetCartCommand(userId);
        final var cartResponse = resultConverter.toGetCartResponse(cartService.getCart(command));
        return ResponseEntity.ok(ApiResponse.ok(cartResponse));
    }
}
