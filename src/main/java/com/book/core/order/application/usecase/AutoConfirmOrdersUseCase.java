package com.book.core.order.application.usecase;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.payment.domain.enums.PaymentState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutoConfirmOrdersUseCase {
    private static final int BATCH_SIZE = 100;
    private static final BigDecimal EARN_RATE = new BigDecimal("0.01");

    private final OrderRepositoryPort orderRepository;
    private final GetPaymentUseCase getPaymentUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final CreateNotificationUseCase createNotificationUseCase;

    @Transactional
    public int execute(final LocalDateTime now) {
        final var orders = orderRepository.findOrdersForAutoConfirmation(now.minusDays(7), BATCH_SIZE);
        for (final var order : orders) {
            final var payment = getPaymentUseCase.execute(order.key());
            if (payment.state() != PaymentState.SUCCESS) {
                continue;
            }
            for (final var item : order.items()) {
                if (!item.isConfirmationDue(now)) {
                    continue;
                }
                final BigDecimal generalPaymentAmount = item.remainingGeneralPaymentAmount();
                item.confirm(now);
                final BigDecimal earnedPoint = generalPaymentAmount.multiply(EARN_RATE).setScale(0, RoundingMode.DOWN);
                earnPointUseCase.execute(order.userId(), PointType.PURCHASE_CONFIRMATION, item.id(), earnedPoint,
                    "purchase-confirmation:" + item.id());
                createNotificationUseCase.execute(new CreateNotificationCommand(order.userId(), "구매가 확정되었습니다.",
                    item.itemName() + " 상품의 구매가 자동으로 확정되었습니다.", "order-auto-confirmation:" + item.id()));
                if (earnedPoint.signum() > 0) {
                    createNotificationUseCase.execute(new CreateNotificationCommand(order.userId(), "구매 확정 포인트 적립",
                        earnedPoint.stripTrailingZeros().toPlainString() + " 포인트가 적립되었습니다.",
                        "point-earned:purchase-confirmation:" + item.id()));
                }
            }
        }
        return orders.size();
    }
}
