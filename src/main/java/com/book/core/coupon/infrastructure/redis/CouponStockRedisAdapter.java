package com.book.core.coupon.infrastructure.redis;

import com.book.core.coupon.application.port.CouponStockPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponStockRedisAdapter implements CouponStockPort {
    private static final RedisScript<Long> COUPON_ISSUE_SCRIPT = RedisScript.of(new ClassPathResource("lua/issue-coupon.lua"), Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void initialize(final Long couponId, final int quantity) {
        redisTemplate.opsForValue().set(stockQuantityKey(couponId), String.valueOf(quantity));
    }

    @Override
    public IssueResult tryIssue(final Long couponId) {
        final Long result = redisTemplate.execute(COUPON_ISSUE_SCRIPT, List.of(stockQuantityKey(couponId)));
        if (result == null) {
            return IssueResult.FAILED;
        }
        return switch (result.intValue()) {
            case 1 -> IssueResult.ISSUED;
            case 0 -> IssueResult.SOLD_OUT;
            default -> IssueResult.FAILED;
        };
    }

    private String stockQuantityKey(final Long couponId) {
        return "coupon:" + couponId + ":stock";
    }
}
