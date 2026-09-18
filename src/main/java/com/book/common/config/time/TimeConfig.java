package com.book.common.config.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    Clock serviceClock() {
        return Clock.system(SERVICE_ZONE);
    }
}
