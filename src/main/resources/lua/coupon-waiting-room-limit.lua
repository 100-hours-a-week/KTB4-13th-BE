local rate = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local now = redis.call('TIME')
local millis = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
local state = redis.call('HMGET', KEYS[1], 'tokens', 'at')
local previous = tonumber(state[2]) or millis
local tokens = math.min(capacity, (tonumber(state[1]) or capacity) + math.max(0, millis - previous) * rate / 1000)
if tokens < 1 then
    return 0
end
redis.call('HSET', KEYS[1], 'tokens', tokens - 1, 'at', math.max(previous, millis))
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity * 1000 / rate))
return 1
