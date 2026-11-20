package com.book.core.order.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.order.application.service.LowStockNotificationMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LowStockNotificationConfigurationTest {
    private final ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(LowStockNotificationConfiguration.class);

    @Test
    void 설정한_단일_모드만_사용한다() {
        contextRunner.withPropertyValues("app.low-stock-notification.mode=V2")
            .run(context -> assertThat(context.getBean(LowStockNotificationMode.class)).isEqualTo(LowStockNotificationMode.V2));
        contextRunner.withPropertyValues("app.low-stock-notification.mode=V3")
            .run(context -> assertThat(context.getBean(LowStockNotificationMode.class)).isEqualTo(LowStockNotificationMode.V3));
    }
}
