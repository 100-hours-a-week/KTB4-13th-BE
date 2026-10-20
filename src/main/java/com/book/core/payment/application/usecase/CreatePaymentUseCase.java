package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.order.domain.Order;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.enums.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;

    @Transactional
    public Long execute(final Order order, final Long payerUserId, final PaymentDiscount paymentDiscount) {
        if (paymentRepository.findByOrderId(order.id()).filter((final Payment item) -> item.state() == PaymentState.SUCCESS).isPresent()) {
            throw new CoreException(ErrorCode.ORDER_ALREADY_PAID);
        }
        // TODO(#327): 정률 쿠폰 비율을 실제 할인 금액과 분리해 결제·취소 잔액에 저장한다.
        final Payment payment = new Payment(order.userId(), order.id(), order.totalPrice(), paymentDiscount.useOwnedCouponId(),
            paymentDiscount.couponDiscount(), paymentDiscount.usePoint(), payerUserId, paymentDiscount.paidAmount(order.totalPrice()),
            PaymentState.READY);
        final Long paymentId = paymentRepository.save(payment).id();
        log.info("결제 생성 처리를 마쳤습니다. paymentId={}, orderId={}, requestId={}", paymentId, order.id(),
            MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY));
        return paymentId;
    }
}
