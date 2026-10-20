package com.book.core.point.infrastructure.scheduler;

import com.book.core.point.application.service.PointExpiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointExpiryScheduler {
    private final PointExpiryService pointExpiryService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void processPointExpiry() {
        pointExpiryService.process();
    }
}
