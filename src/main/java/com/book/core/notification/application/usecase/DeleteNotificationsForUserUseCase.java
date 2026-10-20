package com.book.core.notification.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteNotificationsForUserUseCase {
    private final NotificationRepositoryPort notificationRepository;

    @Transactional
    public void execute(final Long userId) {
        notificationRepository.deleteAllByUserId(userId);
    }
}
