-- KEYS: stream / ARGV: group, consumer, reclaim cursor
local claimed = redis.call('XAUTOCLAIM', KEYS[1], ARGV[1], ARGV[2], 1000, ARGV[3], 'COUNT', 32)
local output = {claimed[1]}
local function append(records)
    for _, entry in ipairs(records) do
        table.insert(output, entry[1])
        table.insert(output, entry[2][2])
    end
end
append(claimed[2])
local new = redis.call('XREADGROUP', 'GROUP', ARGV[1], ARGV[2], 'COUNT', 32, 'STREAMS', KEYS[1], '>')
if new then
    append(new[1][2])
end
return output
