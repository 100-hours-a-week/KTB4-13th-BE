package com.book.core.payment.application.service;

import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.order.domain.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.usecase.CompletePaymentUseCase;
import com.book.core.payment.application.usecase.CreatePaymentUseCase;
import com.book.core.payment.application.usecase.PostProcessPaymentUseCase;
import com.book.core.payment.application.usecase.ValidatePaymentUseCase;
import com.book.core.payment.domain.PaymentCoupon;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final GetCouponTargetsUseCase getCouponTargetsUseCase;
    private final FindOwnedCouponsForCheckoutUseCase findOwnedCouponsForCheckoutUseCase;
    private final GetPointBalanceUseCase getPointBalanceUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;
    private final ValidatePaymentUseCase validatePaymentUseCase;
    private final CompletePaymentUseCase completePaymentUseCase;
    private final PostProcessPaymentUseCase postProcessPaymentUseCase;

    public Long create(final CreatePaymentCommand command) {
        final Order order = getPaymentOrderUseCase.execute(command.userId(), command.orderKey());
        final Set<Long> couponIds = getCouponTargetsUseCase.execute(order.items().stream().map(OrderItem::productId).toList());
        final List<PaymentCoupon> ownedCoupons =
            findOwnedCouponsForCheckoutUseCase.execute(command.userId(), couponIds, LocalDateTime.now()).stream()
                .map((final OwnedCouponResult item) -> new PaymentCoupon(item.id(), item.coupon().type(), item.coupon().discount(),
                    item.coupon().minOrderAmount()))
                .toList();
        final BigDecimal pointBalance = getPointBalanceUseCase.execute(command.userId());
        final PaymentDiscount paymentDiscount =
            new PaymentDiscount(ownedCoupons, pointBalance, command.useOwnedCouponId(), command.usePoint());
        return createPaymentUseCase.execute(order, command.userId(), paymentDiscount);
    }

    /** 승인 금액을 확인하고 결제, 주문, 혜택, 취소 잔액과 이력을 원자적으로 변경한 뒤 비동기 후처리를 요청합니다. */
    public Long success(final ApprovePaymentCommand command) {
        final var order = getPaymentOrderUseCase.execute(command.orderKey(), OrderStatus.CREATED);
        validatePaymentUseCase.validateForApprove(order.id(), command.amount());
        // NOTE: PG 승인 API 호출 => 성공 시 다음 로직으로 진행 | 실패 시 예외 발생
        final Long paymentId = completePaymentUseCase.execute(command.orderKey(), command.externalPaymentKey());
        postProcessPaymentUseCase.execute(paymentId, command.orderKey());
        return paymentId;
    }

}
