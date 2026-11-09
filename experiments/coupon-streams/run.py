"""Run only against newly created, labelled local test containers."""
import base64
import hashlib
import hmac
import json
import math
import os
from pathlib import Path
import re
import signal
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
DATA = Path(sys.argv[1]).resolve()
ENV = dict(os.environ)
DOCKER = ["docker", "--context", ENV.get("DOCKER_CONTEXT", "desktop-linux")]
STREAM = "coupon:issuance:stream"
GROUP = "coupon-db"
DB = ENV["DB_CONTAINER"]
REDIS = ENV["REDIS_CONTAINER"]
APPS = []
COUPON_BASE = int(ENV.get("COUPON_BASE", "530000"))
RATE = int(ENV.get("LOAD_RATE", "500"))


def command(args, **kwargs):
    return subprocess.check_output(args, text=True, **kwargs).strip()


def mysql(sql):
    return command(DOCKER + ["exec", DB, "sh", "-c",
                             'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B book -e "$1"', "_", sql])


def redis(*args):
    return command(DOCKER + ["exec", REDIS, "redis-cli", "--raw", *map(str, args)])


def wait_for(action, seconds=45):
    until = time.monotonic() + seconds
    while time.monotonic() < until:
        if (value := action()):
            return value
        time.sleep(0.1)
    raise AssertionError("condition timed out")


def start(name, profiles="local", jar="streams", workers=1):
    with socket.socket() as port_socket:
        port_socket.bind(("127.0.0.1", 0))
        port = port_socket.getsockname()[1]
    env = ENV | {
        "DB_URL": f"jdbc:mysql://127.0.0.1:{ENV['DB_PORT']}/book",
        "DB_USERNAME": "book", "DB_PASSWORD": "streams_test",
        "REDIS_HOST": "127.0.0.1", "KAKAO_REST_API_KEY": "local",
        "KAKAO_REDIRECT_URI": "http://127.0.0.1", "KAKAO_ADMIN_KEY": "local",
        "SPRING_FLYWAY_LOCATIONS": f"classpath:db/migration,filesystem:{ROOT}/src/test/resources/db/migration",
        "SPRING_PROFILES_ACTIVE": profiles,
    }
    with (DATA / f"{name}.app.log").open("w") as log:
        app = subprocess.Popen(["java", "-XX:ActiveProcessorCount=4", "-Xmx768m", "-jar", str(DATA / f"{jar}.jar"),
                                "--server.address=127.0.0.1", f"--server.port={port}",
                                "--logging.level.root=WARN", "--logging.level.com.book=WARN",
                                "--logging.level.org.springframework=WARN", "--logging.level.org.springframework.web=WARN",
                                "--logging.level.org.springframework.security=WARN",
                                f"--coupon.streams.workers={workers}",
                                "--spring.datasource.hikari.maximum-pool-size=10"], env=env, stdout=log, stderr=log)
    APPS.append(app)
    base = f"http://127.0.0.1:{port}"

    def ready():
        if app.poll() is not None:
            raise RuntimeError(f"application failed: {name}.app.log")
        try:
            return request(base + "/api/v1/health")[0] == 200
        except OSError:
            return False

    wait_for(ready, 90)
    return app, base


def stop(app, abrupt=False):
    if app.poll() is None:
        app.send_signal(signal.SIGKILL if abrupt else signal.SIGTERM)
        app.wait(timeout=20)


def request(url, token=None):
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    req = urllib.request.Request(url, method="POST" if token else "GET", headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=5) as response:
            return response.status, response.read().decode()
    except urllib.error.HTTPError as response:
        return response.code, response.read().decode()


def jwt(user):
    def encode(data):
        return base64.urlsafe_b64encode(data).rstrip(b"=")
    now = int(time.time())
    header = encode(b'{"alg":"HS512","typ":"JWT"}')
    payload = encode(json.dumps(dict(sub=str(user), iat=now, exp=now + 3600,
                                     tokenType="ACCESS", jti=str(user))).encode())
    unsigned = header + b"." + payload
    return (unsigned + b"." + encode(hmac.digest(base64.b64decode(ENV["JWT_SECRET"]), unsigned, hashlib.sha512))).decode()


def issue(base, coupon, user):
    status, body = request(f"{base}/api/v1/coupons/{coupon}/issue", jwt(user))
    return status, json.loads(body).get("code")


def fixture(coupon, stock):
    mysql(f"""INSERT INTO coupon (id,name,type,discount,max_use_count,min_order_amount,starts_at,expired_at,total_quantity,issued_quantity)
      VALUES ({coupon},'streams experiment','FIXED_AMOUNT',1000,1,0,NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,{stock},0)""")
    redis("SET", f"coupon:{coupon}:stock", stock)


def counts(coupon):
    db = mysql(f"""SELECT issued_quantity, (SELECT COUNT(*) FROM owned_coupon WHERE coupon_id={coupon}),
        (SELECT COUNT(DISTINCT user_id) FROM owned_coupon WHERE coupon_id={coupon}), total_quantity
        FROM coupon WHERE id={coupon}""")
    issued, owned, distinct, total = map(int, db.split())
    return dict(issued=issued, owned=owned, distinct=distinct, total=total, stock=int(redis("GET", f"coupon:{coupon}:stock")))


def verify(coupon, expected):
    result = counts(coupon)
    assert result["issued"] == result["owned"] == result["distinct"] == expected, result
    assert result["total"] == result["stock"] + expected, result
    return result


def group():
    rows = redis("XINFO", "GROUPS", STREAM).splitlines()
    if not rows or rows[0].startswith("ERR"):
        return 0, 0
    pairs = dict(zip(rows[::2], rows[1::2]))
    return int(pairs["lag"]), int(pairs["pending"])


def metrics(base):
    _, body = request(base + "/actuator/prometheus")
    data = {}
    for line in body.splitlines():
        if line.startswith(("hikaricp_connections_", "coupon_issuance_")):
            key, value = line.rsplit(" ", 1)
            data[key] = float(value)
    return data


def value(data, name):
    return sum(v for k, v in data.items() if k == name or k.startswith(name + "{"))


def hikari(before, after):
    count = value(after, "hikaricp_connections_acquire_seconds_count") - value(before, "hikaricp_connections_acquire_seconds_count")
    duration = value(after, "hikaricp_connections_acquire_seconds_sum") - value(before, "hikaricp_connections_acquire_seconds_sum")
    return dict(acquisitions=int(count), mean_ms=1000 * duration / count if count else None)


def redis_stats():
    stats = redis("INFO", "commandstats")
    return {name: (int(calls), int(us)) for name, calls, us in
            re.findall(r"cmdstat_(\w+):calls=(\d+),usec=(\d+)", stats)}


def redis_cpu():
    return sum(map(float, re.findall(r"^used_cpu_(?:sys|user):([\d.]+)", redis("INFO", "cpu"), re.MULTILINE)))


def compare_redis(before, after):
    result = {}
    for name in ("evalsha", "xadd", "xreadgroup", "xautoclaim", "xack"):
        calls, us = (a - b for a, b in zip(after.get(name, (0, 0)), before.get(name, (0, 0))))
        result[name] = dict(calls=calls, mean_us=us / calls if calls else None)
    return result


def commit_p99_bucket(before, after, count):
    buckets = {}
    for key, current in after.items():
        if key.startswith("coupon_issuance_commit_delay_seconds_bucket{"):
            upper = float(re.search(r'le="([^"]+)"', key).group(1))
            buckets[upper] = current - before.get(key, 0)
    assert buckets and max(buckets.values()) == count, buckets
    lower = 0
    for upper, cumulative in sorted(buckets.items()):
        if cumulative >= math.ceil(count * 0.99):
            return [lower, upper if math.isfinite(upper) else None]
        lower = upper
    raise AssertionError("commit histogram incomplete")


def lock_totals():
    rows = mysql("""SELECT UPPER(VARIABLE_NAME), VARIABLE_VALUE FROM performance_schema.global_status
        WHERE VARIABLE_NAME IN ('Innodb_row_lock_waits', 'Innodb_row_lock_time')""")
    result = {key: int(value) for key, value in (line.split() for line in rows.splitlines())}
    assert len(result) == 2, result
    return result


def coupon_lock_waits(coupons):
    ids = ", ".join(f"'{coupon}'" for coupon in coupons)
    rows = mysql(f"""SELECT requested.LOCK_DATA, COUNT(DISTINCT requested.ENGINE_LOCK_ID)
        FROM performance_schema.data_lock_waits waits
        JOIN performance_schema.data_locks requested
            ON requested.ENGINE = waits.ENGINE AND requested.ENGINE_LOCK_ID = waits.REQUESTING_ENGINE_LOCK_ID
        JOIN performance_schema.data_locks blocking
            ON blocking.ENGINE = waits.ENGINE AND blocking.ENGINE_LOCK_ID = waits.BLOCKING_ENGINE_LOCK_ID
        WHERE requested.OBJECT_SCHEMA = 'book' AND requested.OBJECT_NAME = 'coupon'
          AND requested.INDEX_NAME = 'PRIMARY' AND requested.LOCK_TYPE = 'RECORD'
          AND requested.LOCK_DATA IN ({ids}) AND blocking.LOCK_DATA = requested.LOCK_DATA
        GROUP BY requested.LOCK_DATA""")
    return {coupon: int(count) for coupon, count in (line.split() for line in rows.splitlines())}


def load(base, label, coupon, expected, rate, seconds, user_base, stream, coupon_count=1, measure_locks=False):
    out = DATA / f"{label}.k6.json"
    env = ENV | dict(BASE_URL=base, COUPON_ID=str(coupon), EXPECTED_STATUS=str(expected),
                     RATE=str(rate), DURATION=str(seconds), USER_BASE=str(user_base), SUMMARY_FILE=str(out),
                     COUPON_COUNT=str(coupon_count))
    before = metrics(base)
    redis_before = redis_stats()
    cpu_before = redis_cpu()
    pending_peak, backlog_peak = 0, 0
    coupons = range(coupon, coupon + coupon_count)
    before_locks = lock_totals() if measure_locks else {}
    lock_samples = []
    start_time = time.monotonic()

    def sample(phase):
        nonlocal backlog_peak
        queue_size = sum(group())
        backlog_peak = max(backlog_peak, queue_size)
        if measure_locks:
            lock_samples.append(dict(phase=phase, elapsed_s=round(time.monotonic() - start_time, 3),
                                     queue=queue_size, rows=coupon_lock_waits(coupons)))
        return queue_size
    with (DATA / f"{label}.k6.log").open("w") as log:
        process = subprocess.Popen(["k6", "run", str(Path(__file__).with_name("load.js"))], env=env, stdout=log, stderr=log)
        while process.poll() is None:
            pending_peak = max(pending_peak, value(metrics(base), "hikaricp_connections_pending"))
            if stream:
                sample("load")
            time.sleep(0.4)
    offered_end = time.monotonic()
    assert process.returncode == 0, f"see {label}.k6.log"
    after = metrics(base)
    if stream:
        if measure_locks:
            until = time.monotonic() + 150
            while sample("drain") != 0:
                assert time.monotonic() < until, "queue did not drain"
                time.sleep(0.25)
        else:
            wait_for(lambda: group() == (0, 0), 150)
    drain_seconds = time.monotonic() - offered_end
    drained = metrics(base)
    data = json.loads(out.read_text())
    completed = int(data["issued_or_accepted"]["values"]["count"])
    dropped = int(data.get("dropped_iterations", {"values": {"count": 0}})["values"]["count"])
    assert completed == int(data["iterations"]["values"]["count"]), data
    final_counts = [verify(coupon, completed)] if coupon_count == 1 else [counts(cid) for cid in coupons]
    assert all(c["issued"] == c["owned"] == c["distinct"] and c["issued"] + c["stock"] == c["total"]
               for c in final_counts), final_counts
    assert sum(c["issued"] for c in final_counts) == completed, final_counts
    count_summary = {key: sum(c[key] for c in final_counts) for key in final_counts[0]}
    row = dict(completed=completed, p99_ms=data["http_req_duration"]["values"]["p(99)"],
               dropped=dropped, elapsed_s=offered_end - start_time,
               hikari_api=hikari(before, after), hikari_through_drain=hikari(before, drained),
               hikari_pending_peak=pending_peak, redis=compare_redis(redis_before, redis_stats()),
               redis_cpu_seconds=redis_cpu() - cpu_before, counts=count_summary)
    if stream:
        row |= dict(queue_peak=backlog_peak, drain_after_k6_s=drain_seconds,
                    commit_p99_bucket_s=commit_p99_bucket(before, drained, completed),
                    commit_count=int(value(drained, "coupon_issuance_commit_delay_seconds_count")
                                     - value(before, "coupon_issuance_commit_delay_seconds_count")))
        assert row["commit_count"] == completed, row
        row["replayed"] = int(value(drained, "coupon_issuance_replayed_total") - value(before, "coupon_issuance_replayed_total"))
        row["worker_failures"] = int(value(drained, "coupon_issuance_worker_failures_total")
                                     - value(before, "coupon_issuance_worker_failures_total"))
    if measure_locks:
        after_locks = lock_totals()
        row["innodb_lock_waits"] = after_locks["INNODB_ROW_LOCK_WAITS"] - before_locks["INNODB_ROW_LOCK_WAITS"]
        row["innodb_lock_wait_ms"] = after_locks["INNODB_ROW_LOCK_TIME"] - before_locks["INNODB_ROW_LOCK_TIME"]
        row["coupon_primary_wait_sample_count"] = sum(bool(s["rows"]) for s in lock_samples)
        row["coupon_primary_sample_count"] = len(lock_samples)
        row["coupon_primary_wait_peak"] = max((sum(s["rows"].values()) for s in lock_samples), default=0)
        row["coupon_counts"] = final_counts
        (DATA / f"{label}.locks.json").write_text(json.dumps(lock_samples, indent=2) + "\n")
    print(label, json.dumps(row), flush=True)
    return row


def perf():
    rows = {}
    for name, coupon, status in (("sync", COUPON_BASE, 200), ("streams", COUPON_BASE + 100, 202)):
        stream = name == "streams"
        profiles = "local,coupon-streams,coupon-streams-worker" if stream else "local"
        app, base = start(name, profiles, name)
        fixture(coupon, 2000)
        load(base, name + "-warm", coupon, status, 200, 5, 7000000000010000, stream)
        fixture(coupon + 1, 20000)
        rows[name] = load(base, name + "-main", coupon + 1, status, RATE, 15, 7000000000020000, stream)
        stop(app)
    return rows


def workers():
    rows = {}
    for index, (worker_count, coupon_count) in enumerate(((1, 1), (2, 1), (4, 1), (4, 4))):
        label = f"workers-{worker_count}-coupons-{coupon_count}"
        coupon = COUPON_BASE + index * 100
        app, base = start(label, "local,coupon-streams,coupon-streams-worker", workers=worker_count)
        fixture(coupon, 2000)
        load(base, label + "-warm", coupon, 202, 200, 5, 7000000000010000, True)
        for cid in range(coupon + 1, coupon + 1 + coupon_count):
            fixture(cid, 20000 // coupon_count)
        rows[label] = load(base, label + "-main", coupon + 1, 202, RATE, 15, 7000000000020000, True,
                           coupon_count=coupon_count, measure_locks=True)
        stop(app)
        (DATA / "workers.json").write_text(json.dumps(rows, indent=2) + "\n")
    return rows


def faults():
    publisher, base = start("fault-publisher", "local,coupon-streams")
    worker_profile = "local,coupon-streams,coupon-streams-worker"
    user = 7000000000040000 + COUPON_BASE
    cases = {}

    coupon = COUPON_BASE + 201
    fixture(coupon, 1)
    redis("RENAME", STREAM, STREAM + ":backup")
    try:
        redis("SET", STREAM, "wrong-type")
        assert issue(base, coupon, user)[0] == 500
        verify(coupon, 0)
        assert redis("EXISTS", f"coupon:{coupon}:sold_out", f"coupon:{coupon}:reservations") == "0"
    finally:
        redis("DEL", STREAM)
        redis("RENAME", STREAM + ":backup", STREAM)
    assert issue(base, coupon, user)[0] == 202
    worker, _ = start("fault-worker", worker_profile)
    wait_for(lambda: group() == (0, 0))
    cases["xadd_failure_then_retry"] = verify(coupon, 1)

    coupon += 1
    fixture(coupon, 2)
    stop(worker, abrupt=True)
    length_before = int(redis("XLEN", STREAM))
    port = int(base.rsplit(":", 1)[1])
    with socket.create_connection(("127.0.0.1", port)) as connection:
        connection.sendall(f"POST /api/v1/coupons/{coupon}/issue HTTP/1.1\r\nHost: 127.0.0.1\r\n"
                           f"Authorization: Bearer {jwt(user)}\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".encode())
    wait_for(lambda: redis("SISMEMBER", f"coupon:{coupon}:reservations", user) == "1")
    assert counts(coupon)["owned"] == 0
    assert issue(base, coupon, user) == (400, "E4001")
    assert int(redis("XLEN", STREAM)) == length_before + 1
    assert counts(coupon)["stock"] == 1
    lost_response_coupon = coupon

    coupon += 1
    fixture(coupon, 1)
    assert issue(base, coupon, user)[0] == 202
    assert group() == (2, 0)
    assert counts(coupon)["issued"] == 0 and counts(coupon)["owned"] == 0
    worker, worker_url = start("worker-restarted", worker_profile)
    wait_for(lambda: group() == (0, 0))
    cases["response_lost_then_retry"] = verify(lost_response_coupon, 1)
    cases["worker_down_then_restart"] = verify(coupon, 1)

    coupon += 1
    fixture(coupon, 1)
    mysql(f"ALTER TABLE owned_coupon ADD CONSTRAINT streams_fail CHECK (user_id <> {user + 1})")
    try:
        assert issue(base, coupon, user + 1)[0] == 202
        wait_for(lambda: value(metrics(worker_url), "coupon_issuance_worker_failures_total") > 0)
        assert group() == (0, 1)
        assert counts(coupon)["issued"] == 0 and counts(coupon)["owned"] == 0
    finally:
        mysql("ALTER TABLE owned_coupon DROP CHECK streams_fail")
    wait_for(lambda: group() == (0, 0))
    cases["temporary_db_failure"] = verify(coupon, 1)

    coupon += 1
    fixture(coupon, 1)
    redis("ACL", "SETUSER", "default", "-xack")
    try:
        assert issue(base, coupon, user)[0] == 202
        wait_for(lambda: value(metrics(worker_url), "coupon_issuance_worker_failures_total") > 1)
        wait_for(lambda: counts(coupon)["owned"] == 1 and group() == (0, 1))
        verify(coupon, 1)
        stop(worker, abrupt=True)
    finally:
        redis("ACL", "SETUSER", "default", "+xack")
    _, worker_url = start("worker-after-ack-crash", worker_profile)
    wait_for(lambda: group() == (0, 0))
    assert value(metrics(worker_url), "coupon_issuance_replayed_total") >= 1
    cases["committed_before_ack_crash"] = verify(coupon, 1)
    print("faults", json.dumps(cases), flush=True)
    stop(publisher)
    return cases


def main():
    for container, container_port, host_port in ((DB, 3306, ENV["DB_PORT"]), (REDIS, 6379, ENV["REDIS_PORT"])):
        label = command(DOCKER + ["inspect", "-f", '{{ index .Config.Labels "book.test" }}', container])
        assert label.startswith("streams-"), container
        assert command(DOCKER + ["port", container, f"{container_port}/tcp"]) == f"127.0.0.1:{host_port}", container
    assert wait_for(lambda: subprocess.run(DOCKER + ["exec", DB, "sh", "-c",
                  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -h127.0.0.1 -uroot -N -e "SELECT 1"'],
                  stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0)
    try:
        results = {}
        for name in sys.argv[2:] or ("perf", "faults"):
            results[name] = {"perf": perf, "faults": faults, "workers": workers}[name]()
            (DATA / f"{name}.json").write_text(json.dumps(results[name], indent=2) + "\n")
    finally:
        for app in APPS:
            stop(app)


if __name__ == "__main__":
    main()
