package com.book.core.order.api;

import com.book.common.response.ApiResponse;
import com.book.core.order.api.converter.OrderCommandConverter;
import com.book.core.order.api.converter.OrderResultConverter;
import com.book.core.order.api.request.CreateOrderRequest;
import com.book.core.order.api.response.CreateOrderResponse;
import com.book.core.order.api.spec.OrderControllerSpec;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
class OrderController implements OrderControllerSpec {
    private final OrderService orderService;
    private final OrderCommandConverter commandConverter;
    private final OrderResultConverter resultConverter;

    @Override
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(@Positive @RequestParam("userId") final Long userId,
        @Valid @RequestBody final CreateOrderRequest request) {
        final var command = commandConverter.toCreateOrderCommand(userId, request);
        final var result = orderService.createOrder(command);
        final var response = resultConverter.toCreateOrderResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @DeleteMapping("/{orderKey}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@NotBlank @Size(max = 255) @PathVariable("orderKey") final String orderKey,
        @Positive @RequestParam("userId") final Long userId) {
        final CancelOrderCommand command = commandConverter.toCancelOrderCommand(userId, orderKey);
        orderService.cancelOrder(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
