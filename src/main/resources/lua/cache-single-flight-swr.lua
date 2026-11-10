-- KEYS[1] = 쿠폰 정책 키 (Hash: value, fetchedAtMs), KEYS[2] = 정책 갱신 락 키
-- KEYS[3] = 실시간 쿠폰 재고 키
-- ARGV: nowMs, freshMs, lockToken, lockTtlMs
-- 반환: HIT (값 반환) | STALE_REFRESH (값 반환 + 백그라운드 refresh)
--        | LOAD (이 요청이 채움) | WAIT (다른 요청이 채우는 중)

-- 반환 배열의 두 번째 원소는 HIT와 STALE_REFRESH에서 정책 JSON이다.
-- HIT와 STALE_REFRESH의 세 번째 원소는 이 조회 시점에 확인한 재고 상태다.
-- LOAD와 WAIT는 반환할 캐시가 없으므로 빈 문자열을 담는다.

-- Redis에서 키가 실제 TTL로 만료되었거나 value 필드가 없으면 캐시 미스로 처리한다.
local data = redis.call('HMGET', KEYS[1], 'value', 'fetchedAtMs')
local value, fetchedAtStr = data[1], data[2]

if not value then
    local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
    -- LOAD: 캐시가 없고 이 요청이 락을 획득했다. 호출자가 동기 로딩과 캐시 저장을 맡는다.
    if acquired then return {'LOAD', ''} end
    -- WAIT: 캐시가 없고 이미 락 소유자가 있다. 호출자가 잠시 기다린 뒤 다시 조회한다.
    return {'WAIT', ''}
end

local stock = redis.pcall('GET', KEYS[3])
local stockState = 'UNKNOWN'
if stock == '0' then
    stockState = 'EXHAUSTED'
elseif type(stock) == 'string' and (tonumber(stock) or 0) > 0 then
    stockState = 'AVAILABLE'
end

local now = tonumber(ARGV[1])
local fetchedAt = tonumber(fetchedAtStr)
-- HIT: 캐시 저장 후 지난 시간이 freshMs보다 짧으면 기존 정책을 즉시 반환한다.
-- 경과 시간이 freshMs와 같아지는 시점부터는 stale 분기로 들어간다.
if now - fetchedAt < tonumber(ARGV[2]) then
    return {'HIT', value, stockState}
end

-- 실제 TTL이 남은 stale 값은 요청에 바로 반환할 수 있다. 갱신 소유자는 락으로 정한다.
local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
-- STALE_REFRESH: stale 값이 있고 이 요청이 락을 획득했다. 호출자는 값을 반환하고 백그라운드 갱신을 시작한다.
if acquired then return {'STALE_REFRESH', value, stockState} end
-- HIT: stale 값이 있고 다른 요청이 락을 보유 중이다. 호출자는 갱신을 기다리지 않고 기존 값을 반환한다.
return {'HIT', value, stockState}
