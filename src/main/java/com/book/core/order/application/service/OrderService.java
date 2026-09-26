package com.book.core.order.application.service;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.usecase.GetAddressUseCase;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.usecase.CreateOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderAddress;
import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final GetAddressUseCase getAddressUseCase;
    private final GetCartUseCase getCartUseCase;
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final CreateOrderUseCase createOrderUseCase;

    public CreateOrderResult createOrder(final CreateOrderCommand command) {
        // 기본 주소가 없어도 주문을 생성하고, 주소 존재 여부는 결제 가능 여부로 반환
        final OrderAddress defaultAddress =
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

        final Order order = Order.create(command.userId(), "order_" + UUID.randomUUID(), defaultAddress);
        for (final CreateOrderItemCommand item : command.items()) {
            final GetProductDetailResult product = productsByProductId.get(item.productId());
            order.addItem(item.productId(), product.itemName(), product.thumbnailUrl(), product.author(), product.salePrice(),
                product.discountedPrice(), item.quantity());
        }
        return createOrderUseCase.execute(order);
    }
}
