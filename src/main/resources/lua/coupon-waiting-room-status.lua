local t = redis.call('TIME')
local now = tonumber(t[1]) * 1000 + math.floor(tonumber(t[2]) / 1000)
local pass = tonumber(redis.call('ZSCORE', KEYS[2], ARGV[1]))
if pass and pass > now then
  return 0
end

local rank = redis.call('ZRANK', KEYS[1], ARGV[1])
if not rank then
  return -1
end
return rank + 1
