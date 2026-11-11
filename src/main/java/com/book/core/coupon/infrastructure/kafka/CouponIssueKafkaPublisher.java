package com.book.core.coupon.infrastructure.kafka;

import static com.book.core.coupon.infrastructure.kafka.CouponIssueKafkaConfig.TOPIC;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("coupon-kafka")
@RequiredArgsConstructor
@Slf4j
class CouponIssueKafkaPublisher {
    private final CouponIssueRequestRepository requests;
    private final KafkaTemplate<String, String> kafka;
    private final PlatformTransactionManager transactions;
    private final MeterRegistry meters;

    @Scheduled(fixedDelayString = "${coupon.kafka.relay-delay-ms:100}", scheduler = "couponKafkaExecutor")
    void relay() {
        try {
            final var transaction = new TransactionTemplate(transactions);
            transaction.executeWithoutResult(status -> publish(requests.lockPublications()));
        } catch (final RuntimeException failure) {
            meters.counter("coupon.issuance.kafka.publish.failures").increment();
            log.atWarn().addKeyValue("errorType", failure.getClass().getSimpleName()).log("[COUPON] Kafka 발행을 다시 시도합니다.");
        }
    }

    private void publish(final List<Long> requestIds) {
        final var deliveries = requestIds.stream().map(requestId -> {
            final String key = requestId.toString();
            return kafka.send(TOPIC, key, key);
        }).toArray(CompletableFuture<?>[]::new);
        CompletableFuture.allOf(deliveries).orTimeout(12, TimeUnit.SECONDS).join();
        requestIds.forEach(requests::published);
    }
}
