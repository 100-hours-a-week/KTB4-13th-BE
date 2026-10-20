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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/** 결제·취소의 주문상품 금액을 적재한다. 참고 구현처럼 재적재의 중복은 허용한다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreateSettlementTargetsUseCase {
    private final SettlementTargetRepositoryPort settlementTargetRepository;
    private final GetOrderItemsForSettlementUseCase getOrderItemsForSettlementUseCase;
    private final GetMerchantProductMappingsUseCase getMerchantProductMappingsUseCase;

    /** 결제 주문상품의 금액을 양수 정산 대상으로 저장한다. */
    @Transactional
    public void processPayments(final LocalDate settleDate, final List<Payment> payments) {
        final Map<Long, Long> transactionIdMap = new HashMap<>();
        payments.forEach((final Payment payment) -> transactionIdMap.put(payment.orderId(), payment.id()));
        final List<OrderItem> orderItems = getOrderItemsForSettlementUseCase.byOrderIds(transactionIdMap.keySet());
        final Map<Long, MerchantProductMapping> merchantMappingMap = merchantMappings(orderItems);
        final List<SettlementTarget> targets = orderItems.stream()
            .filter((final OrderItem item) -> merchantMappingMap.containsKey(item.productId()))
            .map((final OrderItem item) -> target(settleDate, item, merchantMappingMap.get(item.productId()).merchantId(),
                TransactionType.PAYMENT, transactionIdMap.get(item.order().id()), item.quantity(), item.totalPrice(), item.totalPrice()))
            .toList();
        settlementTargetRepository.saveAll(targets);
        log.debug("[SETTLEMENT] 정산 대상 저장 요청 settleDate: {}, count: {}", settleDate, targets.size());
    }

    /** 전체 취소와 부분 취소의 금액을 음수 정산 대상으로 저장한다. */
    @Transactional
    public void processCancels(final LocalDate settleDate, final List<Cancel> cancels) {
        final List<Cancel> allCancels = cancels.stream().filter((final Cancel cancel) -> cancel.type() == CancelType.ALL).toList();
        final List<SettlementTarget> cancelTargets = processAllCancels(settleDate, allCancels);
        final List<Cancel> partialCancels = cancels.stream().filter((final Cancel cancel) -> cancel.type() == CancelType.PARTIAL).toList();
        final List<SettlementTarget> targets = new ArrayList<>(cancelTargets);
        targets.addAll(processPartialCancels(settleDate, partialCancels));
        settlementTargetRepository.saveAll(targets);
        log.debug("[SETTLEMENT] 정산 대상 저장 요청 settleDate: {}, count: {}", settleDate, targets.size());
    }

    private List<SettlementTarget> processAllCancels(final LocalDate settleDate, final List<Cancel> cancels) {
        final Map<Long, Long> transactionIdMap = new HashMap<>();
        cancels.forEach((final Cancel cancel) -> transactionIdMap.put(cancel.orderId(), cancel.id()));
        final List<OrderItem> orderItems = getOrderItemsForSettlementUseCase.byOrderIds(transactionIdMap.keySet());
        final Map<Long, MerchantProductMapping> merchantMappingMap = merchantMappings(orderItems);
        return orderItems.stream().filter((final OrderItem item) -> merchantMappingMap.containsKey(item.productId()))
            .map((final OrderItem item) -> target(settleDate, item, merchantMappingMap.get(item.productId()).merchantId(),
                TransactionType.CANCEL, transactionIdMap.get(item.order().id()), item.quantity(), item.totalPrice(),
                item.totalPrice().negate()))
            .toList();
    }

    private List<SettlementTarget> processPartialCancels(final LocalDate settleDate, final List<Cancel> cancels) {
        if (cancels.isEmpty()) {
            return List.of();
        }
        final List<OrderItem> orderItems =
            getOrderItemsForSettlementUseCase.byOrderItemIds(cancels.stream().map(Cancel::orderItemId).toList());
        final Map<Long, OrderItem> orderItemsById = orderItems.stream().collect(Collectors.toMap(OrderItem::id, Function.identity()));
        final Map<Long, MerchantProductMapping> merchantMappingMap = merchantMappings(orderItems);
        return cancels.stream().filter((final Cancel cancel) -> {
            final OrderItem item = orderItemsById.get(cancel.orderItemId());
            return item != null && merchantMappingMap.containsKey(item.productId());
        }).map((final Cancel cancel) -> {
            final OrderItem item = orderItemsById.get(cancel.orderItemId());
            final MerchantProductMapping merchantMapping = merchantMappingMap.get(item.productId());
            final BigDecimal totalPrice = item.unitPrice().multiply(BigDecimal.valueOf(cancel.canceledQuantity()));
            return target(settleDate, item, merchantMapping.merchantId(), TransactionType.PARTIAL_CANCEL, cancel.id(),
                cancel.canceledQuantity(), totalPrice, totalPrice.negate());
        }).toList();
    }

    private Map<Long, MerchantProductMapping> merchantMappings(final List<OrderItem> orderItems) {
        final Set<Long> productIds = orderItems.stream().map(OrderItem::productId).collect(Collectors.toSet());
        final Map<Long, MerchantProductMapping> merchantMappingMap = new HashMap<>();
        getMerchantProductMappingsUseCase.execute(productIds)
            .forEach((final MerchantProductMapping mapping) -> merchantMappingMap.put(mapping.productId(), mapping));
        return merchantMappingMap;
    }

    private SettlementTarget target(final LocalDate settleDate, final OrderItem item, final Long merchantId,
        final TransactionType transactionType, final Long transactionId, final long quantity, final BigDecimal totalPrice,
        final BigDecimal targetAmount) {
        return new SettlementTarget(merchantId, settleDate, targetAmount, transactionType, transactionId, item.order().id(),
            item.productId(), quantity, item.unitPrice(), totalPrice);
    }
}
