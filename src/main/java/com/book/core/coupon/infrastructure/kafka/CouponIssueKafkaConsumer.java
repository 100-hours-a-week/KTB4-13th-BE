package com.book.core.coupon.infrastructure.kafka;

import static com.book.core.coupon.infrastructure.kafka.CouponIssueKafkaConfig.TOPIC;

import com.book.core.coupon.application.usecase.PersistAcceptedCouponIssueUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("coupon-kafka")
@RequiredArgsConstructor
class CouponIssueKafkaConsumer {
    private final CouponIssueRequestRepository requests;
    private final PersistAcceptedCouponIssueUseCase persistence;

    @KafkaListener(topics = TOPIC, groupId = "${coupon.kafka.group:book-coupon-issuance}", concurrency = "${coupon.kafka.workers:1}",
        ackMode = "RECORD", properties = "auto.offset.reset=earliest")
    @Transactional
    public void receive(final String payload) {
        final long requestId = Long.parseLong(payload);
        requests.lockPending(requestId).ifPresent(issue -> {
            persistence.execute(issue);
            requests.complete(requestId, issue.couponId());
        });
    }
}
