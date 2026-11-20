#!/usr/bin/env bash
# 신간 등록 배치를 두 인스턴스(18081, 18082)로 띄워 같은 시각에 실행합니다.
# 필요한 환경 변수: NATIONAL_LIBRARY_ISBN_API_KEY, DATA_FOR_LIBRARY_API_KEY
# 선택 환경 변수: NEW_BOOK_CRON(기본 2분마다), DATA_FOR_LIBRARY_DAILY_LIMIT(기본 10)
set -euo pipefail

cd "$(dirname "$0")"
JAR="${JAR:-../../../build/libs/book.jar}"
CRON="${NEW_BOOK_CRON:-0 */2 * * * *}"
LIMIT="${DATA_FOR_LIBRARY_DAILY_LIMIT:-10}"

: "${NATIONAL_LIBRARY_ISBN_API_KEY:?국중도 API 키가 필요합니다}"
: "${DATA_FOR_LIBRARY_API_KEY:?정보나루 API 키가 필요합니다}"
export JWT_SECRET="${JWT_SECRET:-MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZg==}"
export KAKAO_REST_API_KEY="${KAKAO_REST_API_KEY:-local-observability}"
export KAKAO_REDIRECT_URI="${KAKAO_REDIRECT_URI:-http://localhost:3000/oauth/kakao/callback}"
mkdir -p logs

start() {
    local name="$1" port="$2"
    java -jar "$JAR" \
        --spring.profiles.active=observability \
        --spring.application.name="$name" \
        --server.port="$port" \
        --spring.datasource.url=jdbc:mysql://127.0.0.1:13306/book \
        --spring.datasource.username=book \
        --spring.datasource.password=localtest \
        --spring.data.redis.port=16379 \
        --spring.flyway.locations=classpath:db/migration,filesystem:../../../src/test/resources/db/migration \
        --app.new-book.enabled=true \
        --app.new-book.collection-cron="$CRON" \
        --app.new-book.detail-daily-limit="$LIMIT" \
        --app.new-book.lock-at-least-for=PT30S \
        --app.new-book.discord-webhook-url= \
        --coupon.kafka.worker-enabled=false \
        --app.profile-sync.enabled=false \
        --app.user-withdrawal.cron=- \
        --app.order-expiration.cron=- \
        > "logs/$name.log" 2>&1 &
    echo "$name: pid $! port $port"
}

start instance-a 18081
start instance-b 18082
echo "종료: kill \$(pgrep -f 'spring.application.name=instance-')"
