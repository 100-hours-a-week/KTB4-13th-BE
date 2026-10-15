package com.book.core.payment.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.usecase.DeleteCartItemsByProductsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.enums.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;

/** 참고 비동기 장바구니 정리와 실패 로그를 유지합니다. */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class PostProcessPaymentUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final DeleteCartItemsByProductsUseCase deleteCartItemsByProductsUseCase;

    /** 커밋된 성공 결제의 주문 상품을 장바구니에서 비동기로 정리합니다. 실패는 기존 오류 로그로 남기고 결제 응답에 전파하지 않습니다. */
    @Async
    public void execute(final Long paymentId, final String orderKey) {
        log.debug("결제 후처리를 시작합니다. paymentId={}, traceId={}", paymentId, MDC.get("traceId"));
        try {
            final var payment = paymentRepository.findById(paymentId).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
            if (payment.state() != PaymentState.SUCCESS) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
            }
            final var order = getPaymentOrderUseCase.execute(orderKey, OrderStatus.PAID);
            deleteCartItemsByProductsUseCase.execute(payment.userId(), order.items().stream().map(OrderItem::productId).toList());
            log.info("결제 후처리를 마쳤습니다. paymentId={}, traceId={}", paymentId, MDC.get("traceId"));
        } catch (final Exception exception) {
            log.error("결제 후처리에 실패했습니다. paymentId={}, orderKey={}", paymentId, orderKey, exception);
        }
    }
}
