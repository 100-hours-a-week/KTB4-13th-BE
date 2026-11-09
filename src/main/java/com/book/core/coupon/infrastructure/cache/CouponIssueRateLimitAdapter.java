package com.book.core.coupon.infrastructure.cache;

import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.longLuaScript;
import static com.book.core.coupon.infrastructure.cache.RedisLuaSupport.runForLong;

import com.book.core.coupon.application.port.CouponIssueRateLimitPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssueRateLimitAdapter implements CouponIssueRateLimitPort {
    private final RedisScript<Long> script = longLuaScript("lua/check-coupon-issue-rate-limit.lua");
    private final StringRedisTemplate redisTemplate;
    private final CouponIssueRateLimitProperties properties;

    @Override
    public long acquire(final Long userId) {
        return runForLong(redisTemplate, script, List.of("coupon:issue-rate-limit:user:" + userId), properties.maxRequests(),
            properties.windowSeconds());
    }
}
