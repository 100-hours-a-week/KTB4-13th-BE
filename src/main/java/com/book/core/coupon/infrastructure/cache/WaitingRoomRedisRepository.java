package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;

import com.book.core.coupon.application.port.WaitingRoomPort;
import java.util.List;
import java.util.OptionalInt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WaitingRoomRedisRepository implements WaitingRoomPort {
    private static final RedisScript<Long> ENTER = longLuaScript("lua/waiting-room-enter.lua");
    private static final RedisScript<Long> STATUS = longLuaScript("lua/waiting-room-status.lua");
    private static final RedisScript<Long> DRAIN = longLuaScript("lua/waiting-room-drain.lua");
    private static final RedisScript<Long> RATE_LIMIT = longLuaScript("lua/coupon-issue-rate-limit.lua");

    private final StringRedisTemplate redis;
    private final WaitingRoomProperties properties;

    @Override
    public OptionalInt admitPerSecond(final long couponId) {
        final var room = properties.rooms().get(couponId);
        return room == null ? OptionalInt.empty() : OptionalInt.of(room.admitPerSecond());
    }

    @Override
    public long enter(final long couponId, final long userId) {
        final String prefix = prefix(couponId);
        return runForLong(redis, ENTER, List.of(prefix + "queue", prefix + "passes", prefix + "sequence"), userId);
    }

    @Override
    public long position(final long couponId, final long userId) {
        final String prefix = prefix(couponId);
        return runForLong(redis, STATUS, List.of(prefix + "queue", prefix + "passes"), userId);
    }

    @Override
    public long retryAfterSeconds(final long userId) {
        return runForLong(redis, RATE_LIMIT, List.of("coupon:issue-rate:user:" + userId), properties.replenishRate(),
            properties.burstCapacity());
    }

    @Scheduled(fixedRate = 100)
    public void drain() {
        properties.rooms().forEach(this::drain);
    }

    long drain(final long couponId, final WaitingRoomProperties.Room room) {
        final String prefix = prefix(couponId);
        return runForLong(redis, DRAIN, List.of(prefix + "queue", prefix + "passes", prefix + "drain"), room.admitPerSecond(),
            room.passTtlMs());
    }

    private String prefix(final long couponId) {
        return "coupon:waiting:{" + couponId + "}:";
    }
}
