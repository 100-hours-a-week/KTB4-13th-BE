package com.book.core.payment.application.service;

import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.OrderItem;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.usecase.CreatePaymentUseCase;
import com.book.core.payment.domain.PaymentCoupon;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 일반 결제와 콜백의 UseCase 실행 순서를 조율합니다. */
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final GetPaymentOrderUseCase getPaymentOrderUseCase;
    private final GetCouponTargetsUseCase getCouponTargetsUseCase;
    private final FindOwnedCouponsForCheckoutUseCase findOwnedCouponsForCheckoutUseCase;
    private final GetPointBalanceUseCase getPointBalanceUseCase;
    private final CreatePaymentUseCase createPaymentUseCase;

    /** 주문 상태와 소유권, 상품 존재 여부를 확인한 뒤 쿠폰 후보와 포인트 잔액으로 결제를 생성합니다. 미입력 포인트 -1 계산을 유지합니다. */
    public Long create(final CreatePaymentCommand command) {
        final var order = getPaymentOrderUseCase.execute(command.userId(), command.orderKey());
        final var couponIds = getCouponTargetsUseCase.execute(order.items().stream().map(OrderItem::productId).toList());
        final var ownedCoupons = findOwnedCouponsForCheckoutUseCase.execute(command.userId(), couponIds, LocalDateTime.now()).stream()
            .map((final var item) -> new PaymentCoupon(item.id(), item.coupon().type(), item.coupon().discount(),
                item.coupon().minOrderAmount()))
            .toList();
        final var pointBalance = getPointBalanceUseCase.execute(command.userId());
        final var paymentDiscount = new PaymentDiscount(ownedCoupons, pointBalance, command.useOwnedCouponId(), command.usePoint());
        return createPaymentUseCase.execute(order, command.userId(), paymentDiscount);
    }

}
