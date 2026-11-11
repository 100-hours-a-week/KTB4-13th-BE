package com.book.core.coupon.infrastructure.cache;

import com.book.core.coupon.application.port.CouponStockPort;
import com.book.core.coupon.application.port.CouponStockSnapshot;
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

    private final StringRedisTemplate redisTemplate;
    private final LoadingCache<String, Boolean> couponSoldOutCache;

    @Override
    public void initialize(final Long couponId, final int quantity) {
        redisTemplate.execute(INITIALIZE_STOCK_SCRIPT, stockKeys(couponId), Integer.toString(quantity));
        couponSoldOutCache.invalidate(soldOutKey(couponId));
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
