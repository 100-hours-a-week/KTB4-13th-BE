package com.book.core.onboarding.infrastructure.kafka;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.profile-sync.kafka-enabled", havingValue = "true")
@Import(ProfileSyncKafkaRelay.class)
@Slf4j
class ProfileSyncKafkaConfig {
    @Bean(defaultCandidate = false, destroyMethod = "shutdown")
    ScheduledExecutorService profileKafkaExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> profileSyncKafkaListenerFactory(
        final ConcurrentKafkaListenerContainerFactoryConfigurer configurer, final ConsumerFactory<Object, Object> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(new DefaultErrorHandler((record, exception) -> {
            log.atWarn().addKeyValue("errorType", exception.getClass().getSimpleName())
                .log("[PROFILE_SYNC] Kafka 소비 재시도를 소진했습니다. MySQL에 남은 작업을 다시 발행합니다.");
        }, new FixedBackOff(1000L, 3L)));
        return factory;
    }
}
