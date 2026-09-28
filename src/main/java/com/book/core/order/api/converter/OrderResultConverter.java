package com.book.core.order.api.converter;

import com.book.core.order.api.response.CreateOrderResponse;
import com.book.core.order.api.response.OrderAddressResponse;
import com.book.core.order.api.response.OrderDetailResponse;
import com.book.core.order.api.response.OrderItemResponse;
import com.book.core.order.api.response.OrderListResponse;
import com.book.core.order.api.response.OrderSummaryResponse;
import com.book.core.order.application.result.GetOrderResult;
import com.book.core.order.application.result.GetOrdersResult;
import com.book.core.order.application.result.OrderItemResult;
import com.book.core.order.application.result.CreateOrderResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderResultConverter {
    public OrderListResponse toOrderListResponse(final GetOrdersResult result) {
        final List<OrderSummaryResponse> orders = result.orders().stream()
            .map(order -> new OrderSummaryResponse(order.key(), order.name(), order.totalPrice(), order.status())).toList();
        return new OrderListResponse(orders, result.nextCursor());
    }

    public OrderDetailResponse toOrderDetailResponse(final GetOrderResult result) {
        final OrderAddressResponse address = result.address() == null ? null
            : new OrderAddressResponse(result.address().postalCode(), result.address().address(), result.address().detailAddress());
        return new OrderDetailResponse(result.key(), result.name(), result.totalPrice(), result.status(),
            toOrderItemResponses(result.items()), address, result.createdAt());
    }

    public CreateOrderResponse toCreateOrderResponse(final CreateOrderResult result) {
        return new CreateOrderResponse(result.orderKey());
    }

    private List<OrderItemResponse> toOrderItemResponses(final List<OrderItemResult> items) {
        return items.stream().map(item -> new OrderItemResponse(item.productId(), item.itemName(), item.thumbnailUrl(), item.author(),
            item.quantity(), item.unitPrice(), item.totalPrice())).toList();
    }
}
