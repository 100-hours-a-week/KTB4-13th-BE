-- KEYS: stock, sold_out / ARGV[1]: sold_out TTL (seconds)
local remainingQuantity = tonumber(redis.call('GET', KEYS[1]) or '0')
if remainingQuantity <= 0 then
  return 0
end
redis.call('DECR', KEYS[1])
if remainingQuantity == 1 then
  redis.call('SET', KEYS[2], '1', 'EX', ARGV[1])
end
return 1
