package com.book.core.payment.application.service;

import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.usecase.FindOwnedCouponsForCheckoutUseCase;
import com.book.core.coupon.application.usecase.GetCouponTargetsUseCase;
import com.book.core.order.application.usecase.GetPaymentOrderUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CouponSelectionCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.command.FailPaymentCommand;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.payment.application.usecase.ApprovePgPaymentUseCase;
import com.book.core.payment.application.usecase.PreparePgOperationUseCase;
import com.book.core.payment.application.usecase.RejectPgOperationUseCase;
import com.book.core.payment.application.usecase.CreatePaymentUseCase;
import com.book.core.payment.application.usecase.FailPaymentUseCase;
import com.book.core.payment.application.usecase.PostProcessPaymentUseCase;
import com.book.core.payment.application.usecase.ValidatePaymentUseCase;
import com.book.core.payment.domain.PaymentCoupon;
import com.book.core.payment.domain.PaymentDiscount;
import com.book.core.payment.domain.PaymentItemPrice;
import com.book.core.point.application.usecase.GetPointBalanceUseCase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final ApprovePgPaymentUseCase approvePgPaymentUseCase;
    private final PreparePgOperationUseCase preparePgOperationUseCase;
    private final RejectPgOperationUseCase rejectPgOperationUseCase;
    private final FailPaymentUseCase failPaymentUseCase;
    private final PostProcessPaymentUseCase postProcessPaymentUseCase;

    public Long create(final CreatePaymentCommand command) {
        final List<CouponSelectionCommand> selections = command.couponSelections() == null ? List.of() : command.couponSelections();
        if (selections.size() > 1 || (command.useOwnedCouponId() > 0 && !selections.isEmpty())) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }
        final Order order = getPaymentOrderUseCase.execute(command.userId(), command.orderKey());
        final Set<Long> couponIds = getCouponTargetsUseCase.execute(order.items().stream().map(OrderItem::productId).toList());
        final List<PaymentCoupon> ownedCoupons =
            findOwnedCouponsForCheckoutUseCase.execute(command.userId(), couponIds, LocalDateTime.now()).stream()
                .map((final OwnedCouponResult item) -> new PaymentCoupon(null, item.id(), item.coupon().id(), item.coupon().type(),
                    item.coupon().discount(), item.coupon().minOrderAmount(), item.coupon().maxDiscountAmount()))
                .toList();
        final BigDecimal pointBalance = getPointBalanceUseCase.execute(command.userId());
        final PaymentDiscount paymentDiscount;
        if (order.items().isEmpty()) {
            paymentDiscount = new PaymentDiscount(ownedCoupons, pointBalance, command.useOwnedCouponId(), command.usePoint());
        } else {
            paymentDiscount = new PaymentDiscount(order.items().stream()
                .map((final OrderItem item) -> new PaymentItemPrice(item.id(), item.unitPrice(), item.quantity())).toList(),
                selectCoupons(command, order.items(), ownedCoupons), pointBalance, command.usePoint());
        }
        return createPaymentUseCase.execute(order, command.userId(), paymentDiscount);
    }

    private List<PaymentCoupon> selectCoupons(final CreatePaymentCommand command, final List<OrderItem> items,
        final List<PaymentCoupon> checkoutCoupons) {
        final List<CouponSelectionCommand> requestedSelections = command.couponSelections() == null ? List.of() : command.couponSelections();
        final Map<Long, PaymentCoupon> couponsById =
            checkoutCoupons.stream().collect(Collectors.toMap(PaymentCoupon::id, Function.identity()));
        final List<CouponSelectionCommand> selections = new java.util.ArrayList<>(requestedSelections);
        if (selections.isEmpty() && command.useOwnedCouponId() > 0) {
            final PaymentCoupon selected = couponsById.get(command.useOwnedCouponId());
            if (selected == null) {
                throw new CoreException(ErrorCode.OWNED_COUPON_INVALID);
            }
            final List<OrderItem> applicableItems = items.stream()
                .filter((final OrderItem item) -> getCouponTargetsUseCase.execute(List.of(item.productId())).contains(selected.couponId()))
                .toList();
            if (applicableItems.size() != 1) {
                throw new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT);
            }
            selections.add(new CouponSelectionCommand(applicableItems.getFirst().id(), selected.id()));
        }
        return selections.stream().map((final CouponSelectionCommand selection) -> {
            final OrderItem item = items.stream().filter((final OrderItem candidate) -> candidate.id().equals(selection.orderItemId()))
                .findFirst().orElseThrow(() -> new CoreException(ErrorCode.PAYMENT_INVALID_AMOUNT));
            final PaymentCoupon coupon = couponsById.get(selection.ownedCouponId());
            if (coupon == null) {
                throw new CoreException(ErrorCode.OWNED_COUPON_INVALID);
            }
            if (!getCouponTargetsUseCase.execute(List.of(item.productId())).contains(coupon.couponId())) {
                throw new CoreException(ErrorCode.OWNED_COUPON_INVALID);
            }
            return new PaymentCoupon(item.id(), coupon.id(), coupon.couponId(), coupon.type(), coupon.discount(), coupon.minOrderAmount(),
                coupon.maxDiscountAmount());
        }).toList();
    }

    public Long success(final ApprovePaymentCommand command) {
        preparePgOperationUseCase.execute(command.operation());
        final Long paymentId;
        try {
            paymentId = approvePgPaymentUseCase.execute(command);
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.PG_REJECTED) {
                rejectPgOperationUseCase.execute(command.operation());
            }
            throw exception;
        }
        postProcessPaymentUseCase.execute(paymentId, command.orderKey());
        return paymentId;
    }

    public void fail(final FailPaymentCommand command) {
        final Order order = getPaymentOrderUseCase.execute(command.orderKey(), OrderStatus.CREATED);
        validatePaymentUseCase.validateForFail(order.id());
        failPaymentUseCase.execute(command.orderKey(), command.code(), command.message());
    }
}
