package com.book.core.order.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.usecase.GetAddressUseCase;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.service.NotificationService;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.result.GetOrderResult;
import com.book.core.order.application.result.GetOrdersResult;
import com.book.core.order.application.usecase.CancelOrderUseCase;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import com.book.core.order.application.usecase.ExpireOrderUseCase;
import com.book.core.order.application.usecase.GetExpiredOrderKeysUseCase;
import com.book.core.order.application.usecase.GetOrderDetailUseCase;
import com.book.core.order.application.usecase.GetOrdersUseCase;
import com.book.core.order.application.usecase.GetReservedOrderQuantitiesUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.ProductQuantitiesCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.result.LowStockReservationResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.application.usecase.ReleaseProductInventoryUseCase;
import com.book.core.product.application.usecase.ReserveProductInventoryUseCase;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final Duration RESERVATION_DURATION = Duration.ofMinutes(10);

    private final GetAddressUseCase getAddressUseCase;
    private final GetCartUseCase getCartUseCase;
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final CreateOrderUseCase createOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final GetOrdersUseCase getOrdersUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final ReserveProductInventoryUseCase reserveProductInventoryUseCase;
    private final ReleaseProductInventoryUseCase releaseProductInventoryUseCase;
    private final GetReservedOrderQuantitiesUseCase getReservedOrderQuantitiesUseCase;
    private final GetExpiredOrderKeysUseCase getExpiredOrderKeysUseCase;
    private final ExpireOrderUseCase expireOrderUseCase;
    private final NotificationService notificationService;

    public GetOrdersResult getOrders(final GetOrdersCommand command) {
        return getOrdersUseCase.execute(command);
    }

    public GetOrderResult getOrder(final GetOrderCommand command) {
        return getOrderDetailUseCase.execute(command);
    }

    @Transactional
    public void cancelOrder(final CancelOrderCommand command) {
        cancelOrderUseCase.execute(command);
        release(getReservedOrderQuantitiesUseCase.execute(command.orderKey()));
    }

    public List<String> getExpiredOrderKeys() {
        return getExpiredOrderKeysUseCase.execute(LocalDateTime.now());
    }

    @Transactional
    public void expireOrder(final String orderKey) {
        release(expireOrderUseCase.execute(orderKey, LocalDateTime.now()));
    }

    @Transactional
    public CreateOrderResult createOrder(final CreateOrderCommand command) {
        final OrderAddress orderAddress =
            getAddressUseCase.execute(command.userId()).map((final GetAddressItemResult address) -> OrderAddress
                .from(address.addressPostalCode(), address.address(), address.detailAddress())).orElse(null);

        // 주문 요청 상품이 사용자의 장바구니에 포함됐는지 확인
        final Set<Long> cartProductIds =
            Set.copyOf(getCartUseCase.execute(command.userId()).items().stream().map(GetCartItemResult::productId).toList());
        final Map<Long, Long> requestedQuantitiesByProductId = new LinkedHashMap<>();
        for (final CreateOrderItemCommand item : command.items()) {
            if (!cartProductIds.contains(item.productId())) {
                throw new CoreException(ErrorCode.PRODUCT_MISMATCH_IN_ORDER);
            }
            requestedQuantitiesByProductId.merge(item.productId(), item.quantity().longValue(), Long::sum);
        }

        // 상품별 재고를 확인하고 주문 시점 상품 정보를 주문 항목에 저장
        final Map<Long, GetProductDetailResult> productsByProductId = new LinkedHashMap<>();
        for (final Map.Entry<Long, Long> entry : requestedQuantitiesByProductId.entrySet()) {
            final GetProductDetailResult product = getProductDetailUseCase.execute(GetProductDetailCommand.of(entry.getKey()));
            if (product.stockQuantity() == null || product.stockQuantity() < 0 || entry.getValue() > product.stockQuantity()) {
                throw new CoreException(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
            }
            productsByProductId.put(entry.getKey(), product);
        }

        final Order order = Order.create(command.userId(), "order_" + UUID.randomUUID(), orderAddress);
        order.reserveUntil(LocalDateTime.now().plus(RESERVATION_DURATION));
        for (final CreateOrderItemCommand item : command.items()) {
            final GetProductDetailResult product = productsByProductId.get(item.productId());
            order.addItem(item.productId(), product.itemName(), product.thumbnailUrl(), product.author(), product.salePrice(),
                product.discountedPrice(), item.quantity());
        }
        final List<LowStockReservationResult> lowStockReservations =
            reserveProductInventoryUseCase.execute(new ProductQuantitiesCommand(order.reservedQuantities()));
        final CreateOrderResult result = createOrderUseCase.execute(order);
        registerLowStockNotifications(lowStockReservations, productsByProductId);
        return result;
    }

    private void registerLowStockNotifications(final List<LowStockReservationResult> lowStockReservations,
        final Map<Long, GetProductDetailResult> productsByProductId) {
        final List<CreateNotificationCommand> commands = lowStockReservations.stream()
            .flatMap((final LowStockReservationResult reservation) -> reservation.userIds().stream().map((final Long userId) -> {
                final String productName = productsByProductId.get(reservation.productId()).itemName();
                final String body = "장바구니에 담아둔 '" + productName + "'의 재고가 " + reservation.availableQuantity() + "권 남았어요.";
                final String eventKey = "cart-low-stock:" + reservation.productId() + ":" + reservation.lowStockCycle() + ":" + userId;
                return new CreateNotificationCommand(userId, "장바구니 상품 품절 임박", body, eventKey);
            })).toList();
        if (commands.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.createAllAsync(commands);
            }
        });
    }

    private void release(final Map<Long, Integer> quantities) {
        if (!quantities.isEmpty()) {
            releaseProductInventoryUseCase.execute(new ProductQuantitiesCommand(quantities));
        }
    }

}
