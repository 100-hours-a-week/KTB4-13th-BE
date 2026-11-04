package com.book.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

class RedisCacheTest {
    private final RedisTemplate<String, String> redisTemplate = mock();
    private final ValueOperations<String, String> values = mock();
    private final RedisCache cache = new RedisCache(redisTemplate, JsonMapper.builder().build());

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(values);
    }

    @Test
    void JSON을_원래_객체로_조회한다() {
        when(values.get("book:1")).thenReturn("{\"name\":\"책\"}");

        assertThat(cache.get("book:1", CachedBook.class)).contains(new CachedBook("책"));
    }

    @Test
    void 값이_없거나_빈_문자열이면_빈_결과를_반환한다() {
        when(values.get("missing")).thenReturn(null);
        when(values.get("empty")).thenReturn("");

        assertThat(cache.get("missing", CachedBook.class)).isEmpty();
        assertThat(cache.get("empty", CachedBook.class)).isEmpty();
    }

    @Test
    void 객체를_JSON으로_변환해_밀리초_만료_시간과_함께_저장한다() {
        cache.set("book:1", new CachedBook("책"), 1500);

        verify(values).set("book:1", "{\"name\":\"책\"}", Duration.ofMillis(1500));
    }

    @Test
    void 양수가_아닌_만료_시간으로_저장하지_않는다() {
        assertThatThrownBy(() -> cache.set("book:1", new CachedBook("책"), 0)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(values);
    }

    @Test
    void 잘못된_JSON과_Redis_오류는_전파한다() {
        when(values.get("bad-json")).thenReturn("{");
        final DataAccessResourceFailureException failure = new DataAccessResourceFailureException("Redis unavailable");
        when(values.get("unavailable")).thenThrow(failure);

        assertThatThrownBy(() -> cache.get("bad-json", CachedBook.class)).isInstanceOf(JacksonException.class);
        assertThatThrownBy(() -> cache.get("unavailable", CachedBook.class)).isSameAs(failure);
    }

    @Test
    void 저장_중_Redis_오류는_전파한다() {
        final DataAccessResourceFailureException failure = new DataAccessResourceFailureException("Redis unavailable");
        doThrow(failure).when(values).set("book:1", "{\"name\":\"책\"}", Duration.ofMillis(1500));

        assertThatThrownBy(() -> cache.set("book:1", new CachedBook("책"), 1500)).isSameAs(failure);
    }

    record CachedBook(String name) {
    }
}
