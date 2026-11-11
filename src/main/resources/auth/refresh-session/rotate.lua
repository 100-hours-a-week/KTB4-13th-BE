local expiry = tonumber(ARGV[3])
local now = redis.call('TIME')
local nowMs = tonumber(now[1]) * 1000 + math.floor(tonumber(now[2]) / 1000)
if not expiry or expiry % 1 ~= 0 or expiry <= nowMs then return -1 end
if #ARGV[1] ~= 64 or string.find(ARGV[1], '[^0-9a-f]') then return -1 end
if #ARGV[2] ~= 64 or string.find(ARGV[2], '[^0-9a-f]') then return -1 end
if redis.call('GET', KEYS[1]) ~= ARGV[1] then return 0 end
redis.call('SET', KEYS[1], ARGV[2], 'PXAT', ARGV[3])
return 1
