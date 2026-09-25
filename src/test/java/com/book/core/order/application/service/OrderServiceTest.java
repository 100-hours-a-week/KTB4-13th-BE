package com.book.core.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderServiceTest {
    private final CreateOrderUseCase createOrderUseCase = mock(CreateOrderUseCase.class);
    private final OrderService service = new OrderService(createOrderUseCase);

    @Test
    void 주문_생성_UseCase를_호출하고_결과를_전달한다() {
        final var command = new CreateOrderCommand(42L, 101L, List.of(new CreateOrderItemCommand(701L, 2)));
        final var result = new CreateOrderResult("order_test");
        when(createOrderUseCase.execute(command)).thenReturn(result);

        assertThat(service.createOrder(command)).isEqualTo(result);

        verify(createOrderUseCase).execute(command);
    }
}
