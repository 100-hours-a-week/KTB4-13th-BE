package com.book.core.payment.application.service;

import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.usecase.CreatePaymentUseCase;
import com.book.core.payment.domain.PaymentCoupon;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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

}
