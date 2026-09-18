package com.book.core.cart.api;

import com.book.common.response.ApiResponse;
import com.book.core.cart.api.request.CartAddRequest;
import com.book.core.cart.api.spec.CartControllerSpec;
import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.usecase.CartAddUseCase;
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
    private final CartAddUseCase addUseCase;

    @PostMapping("/items")
    @Override
    public ResponseEntity<ApiResponse<Void>> add(
            @Positive @RequestParam("userId") final long userId, @Valid @RequestBody final CartAddRequest request) {
        addUseCase.execute(new CartAddCommand(userId, request.productId(), request.quantityOrDefault()));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
