package com.book.core.cancel.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.CancelBalance;
import com.book.core.cancel.domain.enums.CancelType;
import com.book.core.coupon.application.usecase.RevertOwnedCouponUseCase;
import com.book.core.order.application.usecase.GetOrderForCancelUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.application.usecase.RecordPaymentHistoryUseCase;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.PaymentHistory;
import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.point.application.usecase.ReclaimPaymentBonusUseCase;
import com.book.core.point.application.usecase.RestorePaymentPointsUseCase;
import com.book.core.point.domain.PointAmount;
import java.time.Clock;
import com.book.core.payment.application.result.CanceledPaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelPaymentUseCase {
    private final GetOrderForCancelUseCase getOrderForCancelUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelBalanceRepositoryPort cancelBalanceRepository;
    private final RevertOwnedCouponUseCase revertOwnedCouponUseCase;
    private final RestorePaymentPointsUseCase restorePaymentPointsUseCase;
    private final ReclaimPaymentBonusUseCase reclaimPaymentBonusUseCase;
    private final CancelRepositoryPort cancelRepository;
    private final RecordPaymentHistoryUseCase recordPaymentHistoryUseCase;
    private final Clock clock;

    @Transactional
    public Long execute(final CancelAction action, final CanceledPaymentResult result) {
        final Order order = getOrderForCancelUseCase.execute(action.orderKey());
        if (order.status() != OrderStatus.PAID) {
            throw new CoreException(ErrorCode.NOT_FOUND_DATA);
        }
        final Payment payment = getPaymentUseCase.execute(order.key());
        final CancelBalance cancelBalance =
            cancelBalanceRepository.findByOrderId(order.id()).orElseThrow(() -> new CoreException(ErrorCode.NOT_FOUND_DATA));
        order.canceled();
        if (payment.hasAppliedCoupon()) {
            revertOwnedCouponUseCase.execute(payment.ownedCouponId());
        }
        restorePaymentPointsUseCase.execute(payment.userId(), payment.id(), payment.usedPoint());
        reclaimPaymentBonusUseCase.execute(payment.userId(), payment.id(), PointAmount.PAYMENT);
        cancelBalance.cancel(payment.paidAmount(), payment.usedPoint(), payment.couponDiscount());
        final Cancel cancel = cancelRepository.save(new Cancel(payment.userId(), CancelType.ALL, payment.orderId(), -1L, payment.id(),
            payment.originAmount(), payment.ownedCouponId(), payment.couponDiscount(), payment.usedPoint(), payment.paidAmount(), -1L,
            payment.paidAmount(), payment.usedPoint(), payment.couponDiscount(), result.transactionKey(),
            result.canceledAt().atZoneSameInstant(clock.getZone()).toLocalDateTime()));
        recordPaymentHistoryUseCase.execute(
            new PaymentHistory(TransactionType.CANCEL, payment.userId(), payment.orderId(), payment.id(), payment.externalPaymentKey(),
                payment.paidAmount(), payment.usedPoint(), payment.couponDiscount(), "취소 성공", cancel.canceledAt()));
        log.atInfo().addKeyValue("cancelId", cancel.id()).addKeyValue("paymentId", payment.id()).addKeyValue("orderId", order.id())
            .log("[PAYMENT_CANCEL] 결제 전체 취소 처리를 마쳤습니다. cancelId={}, paymentId={}, orderId={}", cancel.id(), payment.id(), order.id());
        return cancel.id();
    }
}
