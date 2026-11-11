package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;

import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.port.CouponIssueQueuePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile({"coupon-streams", "coupon-kafka"})
@RequiredArgsConstructor
public class CouponIssueStreamAdapter implements CouponIssueQueuePort {
    // shortcut: 접수 기록을 자동 삭제하지 않습니다. 운영 트래픽에서는 처리 완료 기준 보존 정책이 필요합니다.
    static final String STREAM = "coupon:issuance:stream";
    private final RedisScript<Long> reserveScript = longLuaScript("lua/reserve-coupon-issue.lua");
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final SoldOutProperties soldOutProperties;

    @Value("${coupon.issuance.stream:coupon:issuance:stream}")
    private String stream;

    @Override
    public Reservation reserve(final AcceptedCouponIssue issue) {
        final String prefix = "coupon:" + issue.couponId() + ":";
        final long result =
            runForLong(redis, reserveScript, List.of(prefix + "stock", prefix + "sold_out", prefix + "reservations", stream),
                issue.userId(), soldOutProperties.ttlSeconds(), objectMapper.writeValueAsString(issue));
        return switch ((int) result) {
            case 1 -> Reservation.ACCEPTED;
            case 0 -> Reservation.SOLD_OUT;
            case -1 -> Reservation.DUPLICATE;
            default -> Reservation.FAILED;
        };
    }
}
