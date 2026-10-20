package com.book.core.order.application.usecase;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotifyOrdersForConfirmationUseCase {
    private static final int BATCH_SIZE = 100;

    private final OrderRepositoryPort orderRepository;
    private final CreateNotificationUseCase createNotificationUseCase;

    @Transactional
    public int execute(final LocalDateTime now) {
        final LocalDateTime reminderDueAfter = now.minusDays(7);
        final LocalDateTime reminderDueBefore = now.minusDays(6);
        final var orders = orderRepository.findOrdersForConfirmationReminder(reminderDueAfter, reminderDueBefore, BATCH_SIZE);
        int sentCount = 0;
        for (final var order : orders) {
            for (final var item : order.items()) {
                if (!item.isConfirmationReminderDue(now) || !item.markConfirmationReminderSent(now)) {
                    continue;
                }
                createNotificationUseCase.execute(new CreateNotificationCommand(order.userId(), "구매 확정 예정 안내",
                    item.itemName() + " 상품이 구매 후 7일이 지나면 자동으로 확정됩니다.", "order-confirmation-reminder:" + item.id()));
                sentCount++;
            }
        }
        return sentCount;
    }
}
