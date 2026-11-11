package com.book.core.coupon.infrastructure.messaging;

import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.usecase.PersistAcceptedCouponIssueUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("coupon-redis-kafka & !coupon-kafka")
@RequiredArgsConstructor
class CouponIssueRedisKafkaConsumer {
    private final ObjectMapper objectMapper;
    private final PersistAcceptedCouponIssueUseCase persistence;

    @KafkaListener(topics = "${coupon.kafka.topic}", groupId = "${coupon.kafka.group:book-coupon-issuance-redis}",
        concurrency = "${coupon.kafka.workers:1}", ackMode = "RECORD", properties = "auto.offset.reset=earliest")
    public void receive(final String payload) {
        persistence.execute(objectMapper.readValue(payload, AcceptedCouponIssue.class));
    }
}
