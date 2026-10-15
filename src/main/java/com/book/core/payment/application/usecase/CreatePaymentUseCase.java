package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.enums.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

/** 참고 PaymentCreator의 결제 생성 및 이미 결제한 주문 검사입니다. */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;

    /** 결제 완료 주문을 거절하고 새 READY 결제를 저장합니다. 기존 READY 결제 재생성은 주문별 UNIQUE 제약으로 거절됩니다. */
    @Transactional
    public Long execute(final Order order, final Long payerUserId, final PaymentDiscount paymentDiscount) {
        if (paymentRepository.findByOrderId(order.id()).filter((final var item) -> item.state() == PaymentState.SUCCESS).isPresent()) {
            throw new CoreException(ErrorCode.ORDER_ALREADY_PAID);
        }
        final Payment payment = new Payment(order.userId(), order.id(), order.totalPrice(), paymentDiscount.useOwnedCouponId(),
            paymentDiscount.couponDiscount(), paymentDiscount.usePoint(), payerUserId, paymentDiscount.paidAmount(order.totalPrice()),
            PaymentState.READY);
        final Long paymentId = paymentRepository.save(payment).id();
        log.info("결제 생성 처리를 마쳤습니다. paymentId={}, orderId={}, traceId={}", paymentId, order.id(), MDC.get("traceId"));
        return paymentId;
    }
}
