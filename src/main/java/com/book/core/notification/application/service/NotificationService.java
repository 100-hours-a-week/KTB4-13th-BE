package com.book.core.notification.application.service;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.result.GetNotificationsResult;
import com.book.core.notification.application.usecase.GetNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final CreateNotificationUseCase createNotificationUseCase;
    private final GetNotificationsUseCase getNotificationsUseCase;

    public Long create(final CreateNotificationCommand command) {
        return createNotificationUseCase.execute(command);
    }

    public GetNotificationsResult getRecent(final GetNotificationsCommand command) {
        return getNotificationsUseCase.execute(command);
    }
}
