local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
local expiry = redis.call('ZSCORE', KEYS[2], ARGV[1])
if expiry and tonumber(expiry) > now then
    return 0
end

local rank = redis.call('ZRANK', KEYS[1], ARGV[1])
if not rank then
    return -1
end
return rank + 1
