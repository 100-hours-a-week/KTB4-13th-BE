-- KEYS: stock, sold_out, reservations / ARGV: userId, sold_out TTL
if redis.call('SISMEMBER', KEYS[3], ARGV[1]) == 1 then
    return -1
end
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
if stock <= 0 then
    return 0
end
redis.call('DECR', KEYS[1])
local reserved = redis.pcall('SADD', KEYS[3], ARGV[1])
if type(reserved) == 'table' and reserved.err then
    redis.call('INCR', KEYS[1])
    return -2
end
if stock == 1 then
    local flagged = redis.pcall('SET', KEYS[2], '1', 'EX', ARGV[2], 'NX')
    if type(flagged) == 'table' and flagged.err then
        redis.call('INCR', KEYS[1])
        redis.call('SREM', KEYS[3], ARGV[1])
        return -2
    end
end
return 1
