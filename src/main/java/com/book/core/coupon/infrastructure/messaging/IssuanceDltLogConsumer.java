package com.book.core.coupon.infrastructure.messaging;

import com.book.core.coupon.application.port.IssuanceDltLogRepositoryPort;
import com.book.core.coupon.domain.IssuanceDltLog;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
@RequiredArgsConstructor
class IssuanceDltLogConsumer {
    private final IssuanceDltLogRepositoryPort logs;

    @KafkaListener(topics = "${coupon.kafka.topic}.DLT", groupId = "${coupon.kafka.dlt-group:book-coupon-issuance-dlt}",
        containerFactory = "issuanceDltListenerFactory", ackMode = "RECORD", properties = "auto.offset.reset=earliest")
    public void consume(final ConsumerRecord<String, String> record) {
        var cause = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN);
        if (cause == null) {
            cause = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN);
        }
        final String errorType = cause == null ? "UNKNOWN" : new String(cause.value(), StandardCharsets.UTF_8);
        logs.record(new IssuanceDltLog(record.topic(), record.partition(), record.offset(), record.key(), record.value(), errorType));
    }
}
