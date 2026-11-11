package com.book.core.coupon.infrastructure.messaging;

import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.usecase.PersistAcceptedCouponIssueUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
@RequiredArgsConstructor
class CouponIssueKafkaConsumer {
    private final ObjectMapper objectMapper;
    private final PersistAcceptedCouponIssueUseCase persistence;

    @KafkaListener(topics = "${coupon.kafka.topic}", groupId = "${coupon.kafka.group:book-coupon-issuance-redis}",
        concurrency = "${coupon.kafka.workers:1}", ackMode = "RECORD", properties = "auto.offset.reset=earliest")
    public void receive(final String payload) {
        persistence.execute(objectMapper.readValue(payload, AcceptedCouponIssue.class));
    }
}
