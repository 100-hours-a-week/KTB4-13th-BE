package com.book.core.coupon.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class CacheConfigTest {
    @Test
    void Caffeine_TTL이_0이면_다음_조회에서_Redis_매진_신호를_다시_읽는다() {
        final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        final var cache = new CacheConfig().couponSoldOutCache(redisTemplate, new SoldOutProperties(86400, 0));
        final String key = "coupon:10:sold_out";
        when(redisTemplate.hasKey(key)).thenReturn(false, true);

        assertThat(cache.get(key)).isFalse();
        assertThat(cache.get(key)).isTrue();
        verify(redisTemplate, times(2)).hasKey(key);
    }
}
