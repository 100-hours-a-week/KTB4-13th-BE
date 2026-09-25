package com.book.core.order.application.service;

import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CreateOrderUseCase createOrderUseCase;

    public CreateOrderResult createOrder(final CreateOrderCommand command) {
        return createOrderUseCase.execute(command);
    }
}
