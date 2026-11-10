package com.book.core.onboarding.infrastructure.kafka;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.profile-sync.kafka-enabled", havingValue = "true")
@Import(ProfileSyncKafkaRelay.class)
class ProfileSyncKafkaConfig {
    @Bean(defaultCandidate = false, destroyMethod = "shutdown")
    ScheduledExecutorService profileKafkaExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
