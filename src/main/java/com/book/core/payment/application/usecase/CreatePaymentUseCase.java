package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.domain.Order;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.OrderItemPaymentAllocation;
import com.book.core.payment.domain.enums.PaymentState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class CreatePaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final OrderRepositoryPort orderRepository;

    public CreatePaymentUseCase(final PaymentRepositoryPort paymentRepository) {
        this(paymentRepository, null);
    }

    @Transactional
    public Long execute(final Order order, final Long payerUserId, final PaymentDiscount paymentDiscount) {
        if (paymentRepository.findByOrderId(order.id()).filter((final Payment item) -> item.state() == PaymentState.SUCCESS).isPresent()) {
            throw new CoreException(ErrorCode.ORDER_ALREADY_PAID);
        }
        for (final OrderItemPaymentAllocation allocation : paymentDiscount.allocations()) {
            order.items().stream().filter((final var item) -> item.id().equals(allocation.orderItemId())).findFirst()
                .orElseThrow(() -> new CoreException(ErrorCode.ORDER_DATA_INCONSISTENT)).applyPaymentAllocation(allocation.ownedCouponId(),
                    allocation.couponType(), allocation.couponDiscountValue(), allocation.couponMinOrderAmount(),
                    allocation.couponMaxDiscountAmount(), allocation.usedPointAmount());
        }
        if (orderRepository != null) {
            orderRepository.save(order);
        }
        final Payment payment = new Payment(order.userId(), order.id(), order.totalPrice(), paymentDiscount.useOwnedCouponId(),
            paymentDiscount.couponDiscount(), paymentDiscount.usePoint(), payerUserId, paymentDiscount.paidAmount(order.totalPrice()),
            PaymentState.READY);
        final Long paymentId = paymentRepository.save(payment).id();
        log.info("결제 생성 처리를 마쳤습니다. paymentId={}, orderId={}, traceId={}", paymentId, order.id(), MDC.get("traceId"));
        return paymentId;
    }
}
