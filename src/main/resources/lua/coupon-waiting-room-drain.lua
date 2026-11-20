local t = redis.call('TIME')
if redis.call('GET', KEYS[4]) == t[1] then
  return 0
end

local users = redis.call('ZRANGE', KEYS[1], 0, tonumber(ARGV[1]) - 1)
if #users == 0 then
  return 0
end

local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
local ttl = tonumber(ARGV[2])
redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', now)
for _, user in ipairs(users) do
  redis.call('ZADD', KEYS[2], now + ttl, user)
  redis.call('ZREM', KEYS[1], user)
end
redis.call('PEXPIRE', KEYS[2], ttl)
redis.call('SET', KEYS[4], t[1], 'EX', 2)
if redis.call('ZCARD', KEYS[1]) == 0 then
  redis.call('DEL', KEYS[3])
end
return #users
