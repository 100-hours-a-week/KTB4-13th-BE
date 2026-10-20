package com.book.core.notification.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PurgeExpiredNotificationsUseCase {
    private static final int RETENTION_DAYS = 90;

    private final NotificationRepositoryPort notificationRepository;
    private final Clock clock;

    @Transactional
    public void execute() {
        notificationRepository.deleteCreatedBefore(LocalDateTime.now(clock).minusDays(RETENTION_DAYS));
    }
}
