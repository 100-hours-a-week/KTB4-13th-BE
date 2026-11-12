package com.book.core.coupon.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class CouponStockAdapterTest {
    private static final List<String> STOCK_KEYS = List.of("coupon:10:stock", "coupon:10:sold_out");

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final LoadingCache<String, Boolean> soldOutCache = mock();
    private final CouponStockAdapter adapter = new CouponStockAdapter(redisTemplate, soldOutCache, new SoldOutProperties(86400, 1000));

    @Test
    void 재고를_초기화할_때_이전_매진_키와_로컬_캐시를_무효화한다() {
        adapter.initialize(10L, 5);

        verify(redisTemplate).execute(any(RedisScript.class), eq(STOCK_KEYS), eq("5"));
        verify(soldOutCache).invalidate("coupon:10:sold_out");
    }

    @Test
    void 매진_신호는_캐시에서_읽는다() {
        when(soldOutCache.get("coupon:10:sold_out")).thenReturn(true, false);

        assertThat(adapter.hasSoldOutSignal(10L)).isTrue();
        assertThat(adapter.hasSoldOutSignal(10L)).isFalse();
    }
}
