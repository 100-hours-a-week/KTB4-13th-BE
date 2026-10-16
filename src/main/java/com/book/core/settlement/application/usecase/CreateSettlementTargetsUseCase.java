package com.book.core.settlement.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.merchant.application.usecase.GetMerchantProductMappingsUseCase;
import com.book.core.merchant.domain.MerchantProductMapping;
import com.book.core.order.application.usecase.GetOrderItemsForSettlementUseCase;
import com.book.core.order.domain.OrderItem;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.settlement.application.port.SettlementTargetRepositoryPort;
import com.book.core.settlement.domain.SettlementTarget;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** 결제·취소의 주문상품 금액을 적재한다. 참고 구현처럼 재적재의 중복은 허용한다. */
@UseCase
@RequiredArgsConstructor
public class CreateSettlementTargetsUseCase {
    private final SettlementTargetRepositoryPort settlementTargetRepository;
    private final GetOrderItemsForSettlementUseCase getOrderItemsForSettlementUseCase;
    private final GetMerchantProductMappingsUseCase getMerchantProductMappingsUseCase;

    @Transactional
    public void processPayments(final LocalDate settleDate, final List<Payment> payments) {
        final Map<Long, Long> transactionIdMap = new HashMap<>();
        payments.forEach((final var payment) -> transactionIdMap.put(payment.orderId(), payment.id()));
        final var orderItems = getOrderItemsForSettlementUseCase.byOrderIds(transactionIdMap.keySet());
        final var merchantMappingMap = merchantMappings(orderItems);
        final var targets = orderItems.stream().filter((final var item) -> merchantMappingMap.containsKey(item.productId()))
            .map((final var item) -> target(settleDate, item, merchantMappingMap.get(item.productId()).merchantId(),
                TransactionType.PAYMENT, transactionIdMap.get(item.order().id()), item.quantity(), item.totalPrice(), item.totalPrice()))
            .toList();
        settlementTargetRepository.saveAll(targets);
    }

    @Transactional
    public void processCancels(final LocalDate settleDate, final List<Cancel> cancels) {
        final var allCancels = cancels.stream().filter((final var cancel) -> cancel.type() == CancelType.ALL).toList();
        final var cancelTargets = processAllCancels(settleDate, allCancels);
        final var partialCancels = cancels.stream().filter((final var cancel) -> cancel.type() == CancelType.PARTIAL).toList();
        final var targets = new ArrayList<>(cancelTargets);
        targets.addAll(processPartialCancels(settleDate, partialCancels));
        settlementTargetRepository.saveAll(targets);
    }

    private List<SettlementTarget> processAllCancels(final LocalDate settleDate, final List<Cancel> cancels) {
        final Map<Long, Long> transactionIdMap = new HashMap<>();
        cancels.forEach((final var cancel) -> transactionIdMap.put(cancel.orderId(), cancel.id()));
        final var orderItems = getOrderItemsForSettlementUseCase.byOrderIds(transactionIdMap.keySet());
        final var merchantMappingMap = merchantMappings(orderItems);
        return orderItems.stream().filter((final var item) -> merchantMappingMap.containsKey(item.productId()))
            .map((final var item) -> target(settleDate, item, merchantMappingMap.get(item.productId()).merchantId(), TransactionType.CANCEL,
                transactionIdMap.get(item.order().id()), item.quantity(), item.totalPrice(), item.totalPrice().negate()))
            .toList();
    }

    private List<SettlementTarget> processPartialCancels(final LocalDate settleDate, final List<Cancel> cancels) {
        if (cancels.isEmpty()) {
            return List.of();
        }
        final var orderItems = getOrderItemsForSettlementUseCase.byOrderItemIds(cancels.stream().map(Cancel::orderItemId).toList());
        final var orderItemsById = orderItems.stream().collect(Collectors.toMap(OrderItem::id, Function.identity()));
        final var merchantMappingMap = merchantMappings(orderItems);
        return cancels.stream().filter((final var cancel) -> {
            final var item = orderItemsById.get(cancel.orderItemId());
            return item != null && merchantMappingMap.containsKey(item.productId());
        }).map((final var cancel) -> {
            final var item = orderItemsById.get(cancel.orderItemId());
            final var merchantMapping = merchantMappingMap.get(item.productId());
            final var totalPrice = item.unitPrice().multiply(BigDecimal.valueOf(cancel.canceledQuantity()));
            return target(settleDate, item, merchantMapping.merchantId(), TransactionType.PARTIAL_CANCEL, cancel.id(),
                cancel.canceledQuantity(), totalPrice, totalPrice.negate());
        }).toList();
    }

    private Map<Long, MerchantProductMapping> merchantMappings(final List<OrderItem> orderItems) {
        final var productIds = orderItems.stream().map(OrderItem::productId).collect(Collectors.toSet());
        final Map<Long, MerchantProductMapping> merchantMappingMap = new HashMap<>();
        getMerchantProductMappingsUseCase.execute(productIds)
            .forEach((final var mapping) -> merchantMappingMap.put(mapping.productId(), mapping));
        return merchantMappingMap;
    }

    private SettlementTarget target(final LocalDate settleDate, final OrderItem item, final Long merchantId,
        final TransactionType transactionType, final Long transactionId, final long quantity, final BigDecimal totalPrice,
        final BigDecimal targetAmount) {
        return new SettlementTarget(merchantId, settleDate, targetAmount, transactionType, transactionId, item.order().id(),
            item.productId(), quantity, item.unitPrice(), totalPrice);
    }
}
