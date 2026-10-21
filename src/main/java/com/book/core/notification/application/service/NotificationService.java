package com.book.core.notification.application.service;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.result.GetNotificationsResult;
import com.book.core.notification.application.usecase.GetNotificationsUseCase;
import com.book.core.notification.application.command.ReadNotificationCommand;
import java.util.List;
import com.book.core.notification.application.usecase.ReadNotificationUseCase;
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

    public GetNotificationsResult getRecent(final GetNotificationsCommand command) {
        return getNotificationsUseCase.execute(command);
    }

    public void read(final ReadNotificationCommand command) {
        readNotificationUseCase.execute(command);
    }
}
