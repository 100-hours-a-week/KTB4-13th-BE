package com.book.core.order.api.converter;

import com.book.core.order.api.response.CreateOrderResponse;
import com.book.core.order.api.response.CreateOrderItemResponse;
import com.book.core.order.application.result.CreateOrderResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderResultConverter {
    public CreateOrderResponse toCreateOrderResponse(final CreateOrderResult result) {
        final List<CreateOrderItemResponse> items = result.items().stream().map(CreateOrderItemResponse::from).toList();
        if (result.canProceedToPayment()) {
            return CreateOrderResponse.created(result.orderKey(), result.totalPrice(), items);
        }
        return CreateOrderResponse.noAddress(result.orderKey(), result.totalPrice(), items);
    }
}
