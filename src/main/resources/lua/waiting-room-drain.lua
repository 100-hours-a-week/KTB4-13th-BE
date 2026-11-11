local users = redis.call('ZRANGE', KEYS[1], 0, tonumber(ARGV[1]) - 1)
if #users == 0 or not redis.call('SET', KEYS[3], '1', 'PX', 1000, 'NX') then
    return 0
end

local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local ttl = tonumber(ARGV[2])
redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', now)
for _, user in ipairs(users) do
    redis.call('ZADD', KEYS[2], now + ttl, user)
    redis.call('ZREM', KEYS[1], user)
end
redis.call('PEXPIRE', KEYS[2], ttl)
return #users
