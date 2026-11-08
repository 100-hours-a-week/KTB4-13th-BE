-- KEYS: 정책 캐시, 갱신 락 / ARGV: 토큰, 정책 JSON(없으면 빈 문자열), 로딩 시각, TTL(ms)
if redis.call('GET', KEYS[2]) ~= ARGV[1] then
    return 0
end
redis.call('DEL', KEYS[1])
if ARGV[2] == '' then
    redis.call('HSET', KEYS[1], 'missing', '1')
else
    redis.call('HSET', KEYS[1], 'value', ARGV[2], 'fetchedAtMs', ARGV[3])
end
redis.call('PEXPIRE', KEYS[1], ARGV[4])
return redis.call('DEL', KEYS[2])
