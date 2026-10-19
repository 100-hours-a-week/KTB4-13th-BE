package com.book.core.notification.application.service;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.usecase.CreateNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final CreateNotificationUseCase createNotificationUseCase;

    public Long create(final CreateNotificationCommand command) {
        return createNotificationUseCase.execute(command);
    }
}
