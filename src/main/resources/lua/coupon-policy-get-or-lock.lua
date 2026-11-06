-- KEYS: policy, lock / ARGV: token, lock TTL in milliseconds
-- Return {status, policy JSON}; only HIT carries a policy.
local cached = redis.call('GET', KEYS[1])
if cached then
    return {'HIT', cached}
end
if redis.call('SET', KEYS[2], ARGV[1], 'NX', 'PX', ARGV[2]) then
    return {'LOAD', ''}
end
return {'WAIT', ''}
