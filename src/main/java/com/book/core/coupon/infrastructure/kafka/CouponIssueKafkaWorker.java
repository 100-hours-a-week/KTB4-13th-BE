package com.book.core.coupon.infrastructure.kafka;

import static com.book.core.coupon.infrastructure.kafka.CouponIssueKafkaConfig.DLT;
import static com.book.core.coupon.infrastructure.kafka.CouponIssueKafkaConfig.TOPIC;

import com.book.core.coupon.application.usecase.PersistAcceptedCouponIssueUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Profile("coupon-kafka")
@RequiredArgsConstructor
@Slf4j
class CouponIssueKafkaWorker {
    private final CouponIssueRequestRepository requests;
    private final PersistAcceptedCouponIssueUseCase persistence;
    private final KafkaTemplate<String, String> kafka;
    private final PlatformTransactionManager transactions;
    private final MeterRegistry meters;

    @Scheduled(fixedDelayString = "${coupon.kafka.relay-delay-ms:100}", scheduler = "couponKafkaExecutor")
    void relay() {
        try {
            final var transaction = new TransactionTemplate(transactions);
            transaction.executeWithoutResult(status -> publish(requests.lockPublications()));
            transaction.executeWithoutResult(status -> publish(requests.lockReplays()));
        } catch (final RuntimeException failure) {
            meters.counter("coupon.issuance.kafka.publish.failures").increment();
            log.atWarn().addKeyValue("errorType", failure.getClass().getSimpleName()).log("[COUPON] Kafka 발행을 다시 시도합니다.");
        }
    }

    private void publish(final java.util.List<CouponIssueRequestRepository.Publication> publications) {
        final var deliveries = publications.stream().map(publication -> {
            final String key = Long.toString(publication.id());
            return kafka.send(TOPIC, key, key + ":" + publication.generation());
        }).toArray(CompletableFuture<?>[]::new);
        CompletableFuture.allOf(deliveries).orTimeout(12, TimeUnit.SECONDS).join();
        publications.forEach(requests::published);
    }

    @KafkaListener(topics = TOPIC, groupId = "${coupon.kafka.group:book-coupon-issuance}", concurrency = "${coupon.kafka.workers:1}",
        ackMode = "RECORD", properties = "auto.offset.reset=earliest")
    @Transactional
    public void receive(final String payload) {
        final var event = Event.parse(payload);
        requests.lockPending(event.id(), event.generation()).ifPresent(issue -> {
            persistence.execute(issue);
            requests.complete(event.id(), issue.couponId());
        });
    }

    @KafkaListener(topics = DLT, groupId = "${coupon.kafka.dlt-group:book-coupon-issuance-dlt}", containerFactory = "couponIssueDltFactory",
        ackMode = "RECORD", properties = "auto.offset.reset=earliest")
    @Transactional
    public void recordFailure(final ConsumerRecord<String, String> record) {
        final var event = Event.parse(record.value());
        final var header = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN);
        final String reason = header == null ? "unknown" : new String(header.value(), StandardCharsets.UTF_8);
        requests.fail(event.id(), event.generation(), record.value(), reason);
    }

    private record Event(long id, int generation) {
        static Event parse(final String payload) {
            final int separator = payload.indexOf(':');
            return new Event(Long.parseLong(payload.substring(0, separator)), Integer.parseInt(payload.substring(separator + 1)));
        }
    }
}
