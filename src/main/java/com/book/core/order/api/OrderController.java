package com.book.core.order.api;

import com.book.common.config.security.CurrentUser;
import com.book.common.response.ApiResponse;
import com.book.core.order.api.converter.OrderCommandConverter;
import com.book.core.order.api.converter.OrderResultConverter;
import com.book.core.order.api.request.CreateOrderRequest;
import com.book.core.order.api.response.CreateOrderResponse;
import com.book.core.order.api.response.OrderDetailResponse;
import com.book.core.order.api.response.OrderListResponse;
import com.book.core.order.api.spec.OrderControllerSpec;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.service.OrderService;
import com.book.core.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/orders")
class OrderController implements OrderControllerSpec {
    private final OrderService orderService;
    private final OrderCommandConverter commandConverter;
    private final OrderResultConverter resultConverter;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<OrderListResponse>> getOrders(@CurrentUser final Long userId,
        @RequestParam(value = "status", required = false) final OrderStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "from", required = false) final LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "to", required = false) final LocalDateTime to,
        @RequestParam(value = "cursor", required = false) final String cursor,
        @RequestParam(value = "limit", required = false) final Integer limit) {
        final var command = commandConverter.toGetOrdersCommand(userId, status, from, to, cursor, limit);
        final var response = resultConverter.toOrderListResponse(orderService.getOrders(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @GetMapping("/{orderKey}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrder(@CurrentUser final Long userId,
        @NotBlank @Size(max = 255) @PathVariable("orderKey") final String orderKey) {
        final var command = commandConverter.toGetOrderCommand(userId, orderKey);
        final var response = resultConverter.toOrderDetailResponse(orderService.getOrder(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(@CurrentUser final Long userId,
        @Valid @RequestBody final CreateOrderRequest request) {
        final var command = commandConverter.toCreateOrderCommand(userId, request);
        final var result = orderService.createOrder(command);
        final var response = resultConverter.toCreateOrderResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @DeleteMapping("/{orderKey}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@CurrentUser final Long userId,
        @NotBlank @Size(max = 255) @PathVariable("orderKey") final String orderKey) {
        final CancelOrderCommand command = commandConverter.toCancelOrderCommand(userId, orderKey);
        orderService.cancelOrder(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
