package com.book.core.order.infrastructure.scheduler;

import com.book.core.order.application.service.OrderConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderConfirmationScheduler {
    private final OrderConfirmationService orderConfirmationService;

    @Scheduled(cron = "0 */15 * * * *", zone = "Asia/Seoul")
    public void confirmDueOrders() {
        orderConfirmationService.processDueConfirmations();
    }
}
