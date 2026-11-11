package com.book.core.coupon.infrastructure.kafka;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
@Profile("coupon-kafka")
class CouponIssueKafkaConfig {
    static final String TOPIC = "book.coupon-issuance.v1";
    static final String DLT = TOPIC + ".DLT";

    @Bean(destroyMethod = "shutdown")
    ScheduledExecutorService couponKafkaExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    NewTopic couponIssueTopic(@Value("${coupon.kafka.partitions:3}") final int partitions,
        @Value("${coupon.kafka.replicas:1}") final int replicas,
        @Value("${coupon.kafka.min-in-sync-replicas:1}") final String minimumReplicas) {
        return TopicBuilder.name(TOPIC).partitions(partitions).replicas(replicas).config("min.insync.replicas", minimumReplicas).build();
    }

    @Bean
    NewTopic couponIssueDlt(@Value("${coupon.kafka.partitions:3}") final int partitions,
        @Value("${coupon.kafka.replicas:1}") final int replicas,
        @Value("${coupon.kafka.min-in-sync-replicas:1}") final String minimumReplicas) {
        return TopicBuilder.name(DLT).partitions(partitions).replicas(replicas).config("min.insync.replicas", minimumReplicas).build();
    }
}
