package com.book.core.coupon.infrastructure.cache;

import java.util.Arrays;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Lua 스크립트 실행 헬퍼. 결과 타입별 캐스팅과 null 처리를 한곳에 모은다.
 */
final class RedisLuaSupport {

    private RedisLuaSupport() {}

    @SuppressWarnings("rawtypes")
    static RedisScript<List> listLuaScript(final String path) {
        return RedisScript.of(new ClassPathResource(path), List.class);
    }

    static RedisScript<Long> longLuaScript(final String path) {
        return RedisScript.of(new ClassPathResource(path), Long.class);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static List<String> runForStrings(
        final StringRedisTemplate redisTemplate,
        final RedisScript<List> script,
        final List<String> keys,
        final Object... args
    ) {
        return (List<String>) redisTemplate.execute(script, keys, toStringArgs(args));
    }

    static long runForLong(
        final StringRedisTemplate redisTemplate,
        final RedisScript<Long> script,
        final List<String> keys,
        final Object... args
    ) {
        final Long result = redisTemplate.execute(script, keys, toStringArgs(args));
        if (result == null) {
            throw new IllegalStateException("Lua 결과 null");
        }
        return result;
    }

    private static Object[] toStringArgs(final Object[] args) {
        return Arrays.stream(args).map(String::valueOf).toArray();
    }
}
