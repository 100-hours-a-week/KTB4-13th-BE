package com.book.core.notification.application.service;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.command.LowStockNotificationEvent;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.notification.application.usecase.CreateLowStockNotificationUseCase;
import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.result.GetNotificationsResult;
import com.book.core.notification.application.usecase.GetNotificationsUseCase;
import com.book.core.notification.application.command.ReadNotificationCommand;
import java.util.List;
import com.book.core.notification.application.usecase.ReadNotificationUseCase;
import com.book.core.product.application.usecase.FindLowStockNotificationCandidatesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final CreateNotificationUseCase createNotificationUseCase;
    private final GetNotificationsUseCase getNotificationsUseCase;
    private final ReadNotificationUseCase readNotificationUseCase;
    private final FindLowStockNotificationCandidatesUseCase findLowStockNotificationCandidatesUseCase;
    private final CreateLowStockNotificationUseCase createLowStockNotificationUseCase;

    public Long create(final CreateNotificationCommand command) {
        return createNotificationUseCase.execute(command);
    }

    @Async
    public void createAllAsync(final List<CreateNotificationCommand> commands) {
        for (final CreateNotificationCommand command : commands) {
            try {
                createNotificationUseCase.execute(command);
            } catch (final RuntimeException exception) {
                log.atError().addKeyValue("userId", command.userId()).log("[NOTIFICATION] 알림 저장에 실패했습니다. userId={}", command.userId(),
                    exception);
            }
        }
    }

    @Async
    public void createLowStockNotificationsAsync(final LowStockNotificationEvent event) {
        final List<Long> candidateUserIds;
        try {
            candidateUserIds = findLowStockNotificationCandidatesUseCase.execute(event.productId(), event.referenceTime());
        } catch (final RuntimeException exception) {
            log.atError().addKeyValue("productId", event.productId()).log("[NOTIFICATION] 재고 부족 알림 후보 조회에 실패했습니다.", exception);
            return;
        }

        for (final Long userId : candidateUserIds) {
            if (userId.equals(event.orderUserId())) {
                continue;
            }
            try {
                if (!createLowStockNotificationUseCase.execute(event, userId)) {
                    return;
                }
            } catch (final RuntimeException exception) {
                log.atError().addKeyValue("productId", event.productId()).addKeyValue("userId", userId)
                    .log("[NOTIFICATION] 재고 부족 알림 저장에 실패했습니다.", exception);
            }
        }
    }

    public GetNotificationsResult getRecent(final GetNotificationsCommand command) {
        return getNotificationsUseCase.execute(command);
    }

    public void read(final ReadNotificationCommand command) {
        readNotificationUseCase.execute(command);
    }
}
