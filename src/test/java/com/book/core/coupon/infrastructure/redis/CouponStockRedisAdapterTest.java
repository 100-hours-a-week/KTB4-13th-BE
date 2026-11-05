package com.book.core.coupon.infrastructure.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.coupon.application.port.IssueResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class CouponStockRedisAdapterTest {
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final CouponStockRedisAdapter adapter = new CouponStockRedisAdapter(redisTemplate);

    @Test
    void 쿠폰_생성_시_재고를_초기화한다() {
        final ValueOperations<String, String> values = mock();
        when(redisTemplate.opsForValue()).thenReturn(values);

        adapter.initialize(10L, 5);

        verify(values).set("coupon:10:stock", "5");
    }

    @Test
    void Lua_스크립트_결과를_발급_상태로_변환한다() {
        when(redisTemplate.execute(any(RedisScript.class), anyList())).thenReturn(1L).thenReturn(0L).thenReturn(2L).thenReturn(null);

        assertThat(adapter.tryIssue(10L)).isEqualTo(IssueResult.ISSUED);
        assertThat(adapter.tryIssue(10L)).isEqualTo(IssueResult.SOLD_OUT);
        assertThat(adapter.tryIssue(10L)).isEqualTo(IssueResult.FAILED);
        assertThat(adapter.tryIssue(10L)).isEqualTo(IssueResult.FAILED);
        verify(redisTemplate, org.mockito.Mockito.times(4)).execute(any(RedisScript.class), eq(java.util.List.of("coupon:10:stock")));
    }
}
