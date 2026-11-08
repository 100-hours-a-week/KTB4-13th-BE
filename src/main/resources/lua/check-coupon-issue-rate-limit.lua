-- KEYS: 사용자 카운터 / ARGV: 요청 한도, 제한 구간(초)
local count = tonumber(redis.call('GET', KEYS[1]) or '0')
if count >= tonumber(ARGV[1]) then
    return math.max(1, math.ceil(redis.call('PTTL', KEYS[1]) / 1000))
end
if redis.call('INCR', KEYS[1]) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return 0
