-- KEYS: stock, sold_out, reservations, stream / ARGV: userId, sold_out TTL, payload
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
local flagged
if stock == 1 then
    flagged = redis.pcall('SET', KEYS[2], '1', 'EX', ARGV[2], 'NX')
end
if not (flagged and flagged.err) then
    local added = redis.pcall('XADD', KEYS[4], '*', 'payload', ARGV[3])
    if type(added) == 'string' then
        return 1
    end
end
-- Redis Lua does not roll back writes if a later command fails.
redis.call('INCR', KEYS[1])
redis.call('SREM', KEYS[3], ARGV[1])
if flagged and flagged.ok then
    redis.call('DEL', KEYS[2])
end
return -2
