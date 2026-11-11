package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;

import com.book.core.coupon.application.port.CouponWaitingRoomPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CouponWaitingRoomAdapter implements CouponWaitingRoomPort {
    private static final RedisScript<Long> ENTER = longLuaScript("lua/coupon-waiting-room-enter.lua");
    private static final RedisScript<Long> STATUS = longLuaScript("lua/coupon-waiting-room-status.lua");
    private static final RedisScript<Long> DRAIN = longLuaScript("lua/coupon-waiting-room-drain.lua");

    private static final RedisScript<Long> LIMIT = longLuaScript("lua/coupon-waiting-room-limit.lua");

    private final StringRedisTemplate redisTemplate;
    private final CouponWaitingRoomProperties properties;

    @Override
    public boolean isEnabled(final long couponId) {
        return properties.couponIds().contains(couponId);
    }

    @Override
    public int admissionsPerSecond() {
        return properties.admissionsPerSecond();
    }

    @Override
    public boolean allowIssueRequest(final long couponId, final long userId) {
        return runForLong(redisTemplate, LIMIT, List.of(keyPrefix(couponId) + ":user:" + userId), properties.requestsPerSecond(),
            properties.requestBurst()) == 1;
    }

    @Override
    public long enter(final long couponId, final long userId) {
        final String prefix = keyPrefix(couponId);
        return runForLong(redisTemplate, ENTER, List.of(prefix + ":queue", prefix + ":passes", prefix + ":sequence"), userId);
    }

    @Override
    public long position(final long couponId, final long userId) {
        final String prefix = keyPrefix(couponId);
        return runForLong(redisTemplate, STATUS, List.of(prefix + ":queue", prefix + ":passes"), userId);
    }

    @Scheduled(fixedDelay = 100)
    void drain() {
        for (final long couponId : properties.couponIds()) {
            final String prefix = keyPrefix(couponId);
            runForLong(redisTemplate, DRAIN, List.of(prefix + ":queue", prefix + ":passes", prefix + ":sequence", prefix + ":tick"),
                properties.admissionsPerSecond(), properties.passTtlMs());
        }
    }

    private String keyPrefix(final long couponId) {
        return "coupon:{" + couponId + "}:waiting";
    }
}
