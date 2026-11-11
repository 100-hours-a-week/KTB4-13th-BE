package com.book.core.coupon.infrastructure.cache;

import java.time.Duration;
import org.springframework.dao.DataAccessException;
import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.CouponStockSnapshot;
import com.book.core.coupon.application.port.IssueResult;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponStockAdapter implements CouponStockPort {
    private static final RedisScript<Long> INITIALIZE_STOCK_SCRIPT =
        RedisScript.of(new ClassPathResource("lua/initialize-coupon-stock.lua"), Long.class);
    private static final RedisScript<Long> COUPON_ISSUE_SCRIPT = RedisScript.of(new ClassPathResource("lua/issue-coupon.lua"), Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SoldOutProperties soldOutProperties;
    private final LoadingCache<String, Boolean> couponSoldOutCache;

    @Override
    public void initialize(final Long couponId, final int quantity) {
        redisTemplate.execute(INITIALIZE_STOCK_SCRIPT, stockKeys(couponId), Integer.toString(quantity));
        couponSoldOutCache.invalidate(soldOutKey(couponId));
    }

    @Override
    public IssueResult tryIssue(final Long couponId) {
        final Long result =
            redisTemplate.execute(COUPON_ISSUE_SCRIPT, stockKeys(couponId), Integer.toString(soldOutProperties.ttlSeconds()));
        if (result == null) {
            return IssueResult.FAILED;
        }
        return switch (result.intValue()) {
            case 1 -> IssueResult.ISSUED;
            case 0 -> IssueResult.SOLD_OUT;
            default -> IssueResult.FAILED;
        };
    }

    @Override
    public void markSoldOut(final long couponId) {
        final String key = soldOutKey(couponId);
        try {
            redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(soldOutProperties.ttlSeconds()));
            couponSoldOutCache.invalidate(key);
        } catch (final DataAccessException exception) {
            // shortcut: DB 매진 판정을 그대로 반환하고 다음 요청에서 신호를 다시 남깁니다.
        }
    }

    @Override
    public boolean hasSoldOutSignal(final Long couponId) {
        return Boolean.TRUE.equals(couponSoldOutCache.get(soldOutKey(couponId)));
    }

    @Override
    public CouponStockSnapshot readSnapshot(final long couponId) {
        final List<String> values = Objects.requireNonNull(redisTemplate.opsForValue().multiGet(stockKeys(couponId)));
        final String stock = values.getFirst();
        return new CouponStockSnapshot(stock == null ? null : Integer.valueOf(stock), values.get(1) != null);
    }

    private List<String> stockKeys(final Long couponId) {
        return List.of("coupon:" + couponId + ":stock", soldOutKey(couponId));
    }

    private String soldOutKey(final Long couponId) {
        return "coupon:" + couponId + ":sold_out";
    }
}
