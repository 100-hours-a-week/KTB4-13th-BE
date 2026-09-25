package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.usecase.GetAddressesUseCase;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateOrderUseCase {
    private final GetAddressesUseCase getAddressesUseCase;
    private final GetCartUseCase getCartUseCase;
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final OrderRepositoryPort orderRepository;

    @Transactional
    public CreateOrderResult execute(final CreateOrderCommand command) {
        final GetAddressItemResult address =
                getAddressesUseCase.execute(new GetAddressesCommand(command.userId())).addresses().stream()
                        .filter(item -> item.addressId().equals(command.addressId()))
                        .findFirst()
                        .orElseThrow(() -> new CoreException(ErrorCode.FORBIDDEN));

        final var cartItems =
                getCartUseCase.execute(new GetCartCommand(command.userId())).items();
        final Map<Long, Long> requestedQuantities = new LinkedHashMap<>();
        for (final var item : command.items()) {
            if (cartItems.stream().noneMatch(cartItem -> cartItem.productId().equals(item.productId()))) {
                throw new CoreException(ErrorCode.PRODUCT_MISMATCH_IN_ORDER);
            }
            requestedQuantities.merge(item.productId(), (long) item.quantity(), Long::sum);
        }

        final Map<Long, GetProductDetailResult> products = new LinkedHashMap<>();
        for (final var entry : requestedQuantities.entrySet()) {
            final GetProductDetailResult product =
                    getProductDetailUseCase.execute(new GetProductDetailCommand(entry.getKey()));
            if (entry.getValue() > product.stockQuantity()) {
                throw new CoreException(ErrorCode.PRODUCT_MISMATCH_IN_ORDER);
            }
            products.put(entry.getKey(), product);
        }

        final var orderAddress =
                OrderAddress.from(address.addressPostalCode(), address.address(), address.detailAddress());
        final var order = Order.create(command.userId(), "order_" + UUID.randomUUID(), orderAddress);
        for (final var item : command.items()) {
            order.addItem(item.productId(), products.get(item.productId()).discountedPrice(), item.quantity());
        }

        final Order savedOrder = orderRepository.save(order);
        return new CreateOrderResult(savedOrder.key());
    }
}
