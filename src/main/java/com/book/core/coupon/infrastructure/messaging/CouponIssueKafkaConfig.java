package com.book.core.coupon.infrastructure.messaging;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
class CouponIssueKafkaConfig {
    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService couponKafkaExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean(defaultCandidate = false, destroyMethod = "shutdown")
    ScheduledExecutorService couponDltReplayExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean(defaultCandidate = false)
    ConcurrentKafkaListenerContainerFactory<Object, Object> issuanceDltListenerFactory(
        final ConcurrentKafkaListenerContainerFactoryConfigurer configurer, final ConsumerFactory<Object, Object> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumerFactory);
        final var errorHandler = new DefaultErrorHandler(new FixedBackOff(5_000L, FixedBackOff.UNLIMITED_ATTEMPTS));
        errorHandler.setClassifications(Map.of(), true);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    NewTopics couponIssueTopics(@Value("${coupon.kafka.topic}") final String topic,
        @Value("${coupon.kafka.partitions:3}") final int partitions, @Value("${coupon.kafka.replicas:1}") final int replicas,
        @Value("${coupon.kafka.min-in-sync-replicas:1}") final String minimumReplicas) {
        return new NewTopics(issueTopic(topic, partitions, replicas, minimumReplicas),
            issueTopic(topic + ".DLT", partitions, replicas, minimumReplicas));
    }

    private NewTopic issueTopic(final String name, final int partitions, final int replicas, final String minimumReplicas) {
        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).config("min.insync.replicas", minimumReplicas).build();
    }
}
