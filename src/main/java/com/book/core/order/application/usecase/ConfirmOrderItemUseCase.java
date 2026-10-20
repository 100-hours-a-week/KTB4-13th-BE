package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.command.ConfirmOrderItemCommand;
import com.book.core.order.application.port.OrderRepositoryPort;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderStatus;
import com.book.core.payment.application.usecase.GetPaymentUseCase;
import com.book.core.payment.domain.enums.PaymentState;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ConfirmOrderItemUseCase {
    private static final BigDecimal EARN_RATE = new BigDecimal("0.01");

    private final OrderRepositoryPort orderRepository;
    private final GetPaymentUseCase getPaymentUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final CreateNotificationUseCase createNotificationUseCase;
    private final Clock clock;

    @Transactional
    public void execute(final ConfirmOrderItemCommand command) {
        final var order = orderRepository.findActiveOrderByKeyForUpdate(command.orderKey())
            .orElseThrow(() -> new CoreException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        if (order.status() != OrderStatus.PAID && order.status() != OrderStatus.PARTIAL_CANCELED) {
            throw new CoreException(ErrorCode.ORDER_CANNOT_BE_CONFIRMED);
        }
        final var payment = getPaymentUseCase.execute(order.key());
        if (payment.state() != PaymentState.SUCCESS) {
            throw new CoreException(ErrorCode.PAYMENT_INVALID_STATE);
        }
        final OrderItem item = order.items().stream().filter((final var candidate) -> candidate.id().equals(command.orderItemId()))
            .findFirst().orElseThrow(() -> new CoreException(ErrorCode.ORDER_ITEM_NOT_FOUND));
        final LocalDateTime now = LocalDateTime.now(clock);
        final BigDecimal generalPaymentAmount = item.remainingGeneralPaymentAmount();
        item.confirm(now);
        final BigDecimal earnedPoint = generalPaymentAmount.multiply(EARN_RATE).setScale(0, RoundingMode.DOWN);
        earnPointUseCase.execute(order.userId(), PointType.PURCHASE_CONFIRMATION, item.id(), earnedPoint,
            "purchase-confirmation:" + item.id());
        if (earnedPoint.signum() > 0) {
            createNotificationUseCase.execute(new CreateNotificationCommand(order.userId(), "구매 확정 포인트 적립",
                earnedPoint.stripTrailingZeros().toPlainString() + " 포인트가 적립되었습니다.", "point-earned:purchase-confirmation:" + item.id()));
        }
    }
}
