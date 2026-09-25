package com.book.core.order.api.converter;

import com.book.core.order.api.response.CreateOrderResponse;
import com.book.core.order.application.result.CreateOrderResult;
import org.springframework.stereotype.Component;

@Component
public class OrderResultConverter {
    public CreateOrderResponse toCreateOrderResponse(final CreateOrderResult result) {
        return new CreateOrderResponse(result.orderKey());
    }
}
