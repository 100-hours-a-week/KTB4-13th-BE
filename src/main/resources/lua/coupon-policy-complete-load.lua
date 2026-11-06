-- KEYS: policy, lock / ARGV: token, optional policy JSON and policy TTL in milliseconds
if redis.call('GET', KEYS[2]) ~= ARGV[1] then
    return 0
end
if ARGV[2] then
    -- Spring Data Redis 4.0 treats -1 ms and -1 s as persistent.
    if ARGV[3] == '-1' or ARGV[3] == '-1000' then
        redis.call('SET', KEYS[1], ARGV[2])
    else
        redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
    end
end
return redis.call('DEL', KEYS[2])
