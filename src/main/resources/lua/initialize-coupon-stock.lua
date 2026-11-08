-- KEYS: stock, sold_out / ARGV[1]: stock quantity
redis.call('SET', KEYS[1], ARGV[1])
return redis.call('DEL', KEYS[2])
