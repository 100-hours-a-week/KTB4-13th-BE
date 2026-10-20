package com.book.core.order.infrastructure.scheduler;

import com.book.core.order.application.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationScheduler {
    private final OrderService orderService;

    @Scheduled(cron = "${app.order-expiration.cron:0 */5 * * * *}", zone = "Asia/Seoul")
    public void expireOrders() {
        try {
            orderService.getExpiredOrderKeys().forEach(this::expireOrder);
        } catch (final Exception exception) {
            log.error("만료 주문 조회에 실패했습니다.", exception);
        }
    }

    private void expireOrder(final String orderKey) {
        try {
            orderService.expireOrder(orderKey);
        } catch (final Exception exception) {
            log.error("주문 예약 만료 처리에 실패했습니다. orderKey={}", orderKey, exception);
        }
    }
}
