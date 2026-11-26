package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;

import com.book.core.coupon.application.command.IssuanceRequested;
import com.book.core.coupon.application.port.CouponIssueQueuePort;
import com.book.core.coupon.application.port.IssuanceRequestPublisher;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
class CouponIssueKafkaAdapter implements CouponIssueQueuePort {
    private final RedisScript<Long> reserveScript = longLuaScript("lua/reserve-coupon-issue.lua");
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SoldOutProperties soldOutProperties;
    private final IssuanceRequestPublisher publisher;

    @Override
    public Reservation reserve(final IssuanceRequested issue) {
        final String payload = objectMapper.writeValueAsString(issue);
        final String prefix = "coupon:" + issue.couponId() + ":";
        final long result =
            runForLong(redis, reserveScript, List.of(prefix + "stock", prefix + "sold_out", prefix + "reservations"),
                issue.userId(), soldOutProperties.ttlSeconds());
        return switch ((int) result) {
            case 1 -> publish(issue, payload);
            case 0 -> Reservation.SOLD_OUT;
            case -1 -> Reservation.DUPLICATE;
            default -> Reservation.FAILED;
        };
    }

    private Reservation publish(final IssuanceRequested issue, final String payload) {
        final String messageKey = issue.couponId() + ":" + issue.userId();
        try {
            publisher.publishAndWait(messageKey, payload);
            return Reservation.ACCEPTED;
        } catch (final RuntimeException failure) {
            log.atWarn().addKeyValue("couponId", issue.couponId()).addKeyValue("userId", issue.userId())
                .addKeyValue("errorType", failure.getClass().getSimpleName()).log("[COUPON] Kafka 발행 미확인, Redis 예약 유지");
            return Reservation.FAILED;
        }
    }
}
