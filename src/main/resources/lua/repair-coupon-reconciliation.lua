-- KEYS: stock, sold_out, reservations / ARGV: stock, sold_out presence, TTL, observed count, observed IDs, missing IDs
local observedCount = tonumber(ARGV[4])
if redis.call('GET', KEYS[1]) ~= ARGV[1]
    or redis.call('EXISTS', KEYS[2]) ~= tonumber(ARGV[2])
    or redis.call('SCARD', KEYS[3]) ~= observedCount then
    return 0
end
for i = 5, 4 + observedCount do
    if redis.call('SISMEMBER', KEYS[3], ARGV[i]) == 0 then
        return 0
    end
end
for i = 5 + observedCount, #ARGV do
    redis.call('SADD', KEYS[3], ARGV[i])
end
if ARGV[1] == '0' then
    redis.call('SET', KEYS[2], '1', 'EX', ARGV[3], 'NX')
else
    redis.call('DEL', KEYS[2])
end
return 1
