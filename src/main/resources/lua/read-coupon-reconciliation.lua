-- KEYS: stock, sold_out, reservations
local result = {redis.call('GET', KEYS[1]) or '', tostring(redis.call('EXISTS', KEYS[2]))}
for _, userId in ipairs(redis.call('SMEMBERS', KEYS[3])) do
    table.insert(result, userId)
end
return result
