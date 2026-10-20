package com.book.core.notification.infrastructure.scheduler;

import com.book.core.notification.application.usecase.PurgeExpiredNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPurgeScheduler {
    private final PurgeExpiredNotificationsUseCase purgeExpiredNotificationsUseCase;

    @Scheduled(cron = "0 */15 * * * *", zone = "Asia/Seoul")
    public void purgeExpiredNotifications() {
        purgeExpiredNotificationsUseCase.execute();
    }
}
