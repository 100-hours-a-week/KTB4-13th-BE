package com.book.core.order.infrastructure.config;

import com.book.core.order.application.service.LowStockNotificationMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class LowStockNotificationConfiguration {
    @Bean
    LowStockNotificationMode lowStockNotificationMode(@Value("${app.low-stock-notification.mode}") final LowStockNotificationMode mode) {
        return mode;
    }
}
