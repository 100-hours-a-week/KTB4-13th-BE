package com.book.core.cart.api;

import com.book.common.response.ApiResponse;
import com.book.core.cart.api.request.CartAddRequest;
import com.book.core.cart.api.request.CartChangeQuantityRequest;
import com.book.core.cart.api.request.CartDeleteRequest;
import com.book.core.cart.api.spec.CartControllerSpec;
import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.command.CartChangeQuantityCommand;
import com.book.core.cart.application.command.CartDeleteCommand;
import com.book.core.cart.application.result.CartQueryResult;
import com.book.core.cart.application.usecase.CartAddUseCase;
import com.book.core.cart.application.usecase.CartChangeQuantityUseCase;
import com.book.core.cart.application.usecase.CartDeleteUseCase;
import com.book.core.cart.application.usecase.CartQueryUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
class CartController implements CartControllerSpec {
    private final CartQueryUseCase queryUseCase;
    private final CartAddUseCase addUseCase;
    private final CartChangeQuantityUseCase changeQuantityUseCase;
    private final CartDeleteUseCase deleteUseCase;

    @GetMapping
    @Override
    public ApiResponse<CartQueryResult> query(@Positive @RequestParam("userId") final long userId) {
        return ApiResponse.ok(queryUseCase.execute(userId));
    }

    @PostMapping("/items")
    @Override
    public ResponseEntity<ApiResponse<Void>> add(
            @Positive @RequestParam("userId") final long userId, @Valid @RequestBody final CartAddRequest request) {
        addUseCase.execute(new CartAddCommand(userId, request.productId(), request.quantityOrDefault()));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/items/{cartItemId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> changeQuantity(
            @Positive @RequestParam("userId") final long userId,
            @Positive @PathVariable final long cartItemId,
            @Valid @RequestBody final CartChangeQuantityRequest request) {
        changeQuantityUseCase.execute(new CartChangeQuantityCommand(userId, cartItemId, request.quantity()));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/items/{cartItemId}")
    @Override
    public ResponseEntity<ApiResponse<Void>> delete(
            @Positive @RequestParam("userId") final long userId, @Positive @PathVariable final long cartItemId) {
        deleteUseCase.execute(new CartDeleteCommand(userId, List.of(cartItemId)));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/items")
    @Override
    public ResponseEntity<ApiResponse<Void>> deleteMany(
            @Positive @RequestParam("userId") final long userId, @Valid @RequestBody final CartDeleteRequest request) {
        deleteUseCase.execute(new CartDeleteCommand(userId, request.cartItemIds()));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
