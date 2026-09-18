package com.book.core.cart.api;

import com.book.common.response.ApiResponse;
import com.book.core.cart.api.converter.CartCommandConverter;
import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.spec.CartControllerSpec;
import com.book.core.cart.application.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

    @PostMapping("/items")
    @Override
    public ResponseEntity<ApiResponse<Void>> addCartItem(
            @Positive @RequestParam("userId") final Long userId,
            @Valid @RequestBody final AddCartItemRequest request
    ) {
        final var command = commandConverter.toAddCartItemCommand(userId, request);
        cartService.addCartItem(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
