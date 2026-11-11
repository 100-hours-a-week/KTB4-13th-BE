local created = redis.pcall('XGROUP', 'CREATE', KEYS[1], ARGV[1], '0-0', 'MKSTREAM')
if created.err and not string.find(created.err, 'BUSYGROUP', 1, true) then
    return created
end
return 1
