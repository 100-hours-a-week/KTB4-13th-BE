package com.book.core.coupon.infrastructure.kafka;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;

@Configuration(proxyBeanMethods = false)
@Profile({"coupon-kafka", "coupon-redis-kafka"})
class CouponIssueKafkaConfig {
    static final String TOPIC = "book.coupon-issuance.v1";

    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService couponKafkaExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    @Profile("coupon-kafka")
    NewTopics couponIssueTopics(@Value("${coupon.kafka.partitions:3}") final int partitions,
        @Value("${coupon.kafka.replicas:1}") final int replicas,
        @Value("${coupon.kafka.min-in-sync-replicas:1}") final String minimumReplicas) {
        return topics(TOPIC, partitions, replicas, minimumReplicas);
    }

    @Bean
    @Profile("coupon-redis-kafka & !coupon-kafka")
    NewTopics couponIssueRedisTopics(@Value("${coupon.kafka.topic}") final String topic,
        @Value("${coupon.kafka.partitions:3}") final int partitions, @Value("${coupon.kafka.replicas:1}") final int replicas,
        @Value("${coupon.kafka.min-in-sync-replicas:1}") final String minimumReplicas) {
        return topics(topic, partitions, replicas, minimumReplicas);
    }

    private NewTopics topics(final String topic, final int partitions, final int replicas, final String minimumReplicas) {
        return new NewTopics(issueTopic(topic, partitions, replicas, minimumReplicas),
            issueTopic(topic + ".DLT", partitions, replicas, minimumReplicas));
    }

    private NewTopic issueTopic(final String name, final int partitions, final int replicas, final String minimumReplicas) {
        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).config("min.insync.replicas", minimumReplicas).build();
    }
}
