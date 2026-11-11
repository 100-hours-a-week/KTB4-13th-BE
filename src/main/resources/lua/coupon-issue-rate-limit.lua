local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local refill = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2]) * 1000
local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'time')
local previous = tonumber(bucket[2]) or now
local tokens = math.min(capacity, (tonumber(bucket[1]) or capacity) + math.max(0, now - previous) * refill)

if tokens < 1000 then
    return math.ceil((1000 - tokens) / refill / 1000)
end
redis.call('HSET', KEYS[1], 'tokens', tokens - 1000, 'time', math.max(now, previous))
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / refill))
return 0
