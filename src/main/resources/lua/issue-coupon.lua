local remainingQuantity = tonumber(redis.call('GET', KEYS[1]) or '0')
if remainingQuantity <= 0 then
  return 0
end
redis.call('DECR', KEYS[1])
return 1
