package com.book.core.order.api.converter;

import com.book.core.order.api.response.CreateOrderResponse;
import com.book.core.order.api.response.CreateOrderItemResponse;
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
        final List<OrderSummaryResponse> orders =
            result.orders().stream().map((final var order) -> new OrderSummaryResponse(order.key(), order.representativeBookName(),
                order.itemCount(), order.thumbnailUrl(), order.totalPrice(), order.status(), order.paidAt())).toList();
        return new OrderListResponse(orders, result.nextCursor());
    }

    public OrderDetailResponse toOrderDetailResponse(final GetOrderResult result) {
        final OrderAddressResponse address = result.address() == null ? null
            : new OrderAddressResponse(result.address().postalCode(), result.address().address(), result.address().detailAddress());
        return new OrderDetailResponse(result.key(), result.status(), result.totalPrice(), result.createdAt(), address,
            toOrderItemResponses(result.items()));
    }

    public CreateOrderResponse toCreateOrderResponse(final CreateOrderResult result) {
        final List<CreateOrderItemResponse> items = result.items().stream().map(CreateOrderItemResponse::from).toList();
        return result.canProceedToPayment() ? CreateOrderResponse.created(result.orderKey(), result.totalPrice(), items)
            : CreateOrderResponse.noAddress(result.orderKey(), result.totalPrice(), items);
    }

    private List<OrderItemResponse> toOrderItemResponses(final List<OrderItemResult> items) {
        return items.stream()
            .map((final var item) -> new OrderItemResponse(item.orderItemId(), item.productId(), item.itemName(), item.thumbnailUrl(),
                item.author(), item.unitPrice(), item.totalPrice(), item.quantity(), item.canceledQuantity(), item.status()))
            .toList();
    }
}
