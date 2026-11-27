#!/usr/bin/env python3
"""main push CI의 이미지 빌드·Push 로그를 구간별로 분해합니다.

읽기 전용 조회입니다. `gh` CLI 인증이 필요합니다. 로그 원문은 저장하지 않고 구간 시각만 기록합니다.

구간:
- gradle_download: Gradle 배포판 다운로드 시작부터 Gradle 시작까지(캐시 부재 지표)
- gradle_boot_jar: 컨테이너 안 `./gradlew clean bootJar` 단계(BuildKit DONE 기준)
- image_export: 이미지 레이어 export
- push: Pushing부터 Pushed까지
- docker_step_total: compose build 시작부터 Pushed까지
"""
import argparse
import json
import re
import statistics
import subprocess
from datetime import datetime

REPO = "100-hours-a-week/KTB4-13th-BE"
JOB_NAME = "Publish backend image"
TIMESTAMP = re.compile(r"^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+)Z (.*)$")


def gh(*args):
    return subprocess.run(["gh", *args], check=True, capture_output=True, text=True).stdout


def parse_time(value):
    return datetime.fromisoformat(value[:26])


def first(lines, pattern):
    regex = re.compile(pattern)
    for time, text in lines:
        if regex.search(text):
            return time, text
    return None, None


def analyze(log):
    lines = []
    for raw in log.splitlines():
        match = TIMESTAMP.match(raw)
        if match:
            lines.append((parse_time(match.group(1)), match.group(2)))

    start, _ = first(lines, r"Run docker compose -f compose\.build\.yml build")
    download, _ = first(lines, r"Downloading https://services\.gradle\.org/distributions/")
    welcome, _ = first(lines, r"Welcome to Gradle")
    _, gradle_done = first(lines, r"\[builder \d+/\d+\] RUN \./gradlew|#\d+ DONE .*s$")
    _, build_line = first(lines, r"BUILD SUCCESSFUL in")
    export_start, _ = first(lines, r"exporting layers$")
    export_done, _ = first(lines, r"exporting layers .* done")
    push_start, _ = first(lines, r" Pushing\s*$")
    pushed, _ = first(lines, r" Pushed\s*$")

    gradle_step = None
    step_match = re.search(r"#(\d+) \[builder \d+/\d+\] RUN \./gradlew", log)
    if step_match:
        done = re.search(rf"#{step_match.group(1)} DONE ([\d.]+)s", log)
        gradle_step = float(done.group(1)) if done else None

    seconds = lambda a, b: (b - a).total_seconds() if a and b else None
    return {
        "gradle_download_seconds": seconds(download, welcome),
        "gradle_boot_jar_seconds": gradle_step,
        "gradle_reported": re.search(r"BUILD SUCCESSFUL in (\S+)", build_line).group(1) if build_line else None,
        "image_export_seconds": seconds(export_start, export_done),
        "push_seconds": seconds(push_start, pushed),
        "docker_step_total_seconds": seconds(start, pushed),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=40)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    runs = json.loads(gh(
        "run", "list", "-R", REPO, "--workflow", "ci.yml", "--event", "push", "--limit", str(args.limit),
        "--json", "databaseId,conclusion,createdAt,headSha",
    ))
    records = []
    for run in runs:
        if run["conclusion"] != "success":
            continue
        jobs = json.loads(gh("api", f"repos/{REPO}/actions/runs/{run['databaseId']}/jobs"))["jobs"]
        job = next((job for job in jobs if job["name"] == JOB_NAME and job["conclusion"] == "success"), None)
        if job is None:
            continue
        try:
            log = gh("api", f"repos/{REPO}/actions/jobs/{job['id']}/logs")
        except subprocess.CalledProcessError:
            records.append({"run_id": run["databaseId"], "created_at": run["createdAt"], "log_unavailable": True})
            continue
        records.append({"run_id": run["databaseId"], "created_at": run["createdAt"], **analyze(log)})

    keys = ["gradle_download_seconds", "gradle_boot_jar_seconds", "image_export_seconds", "push_seconds",
            "docker_step_total_seconds"]
    summary = {}
    for key in keys:
        values = sorted(record[key] for record in records if record.get(key) is not None)
        if values:
            quartiles = statistics.quantiles(values, n=4) if len(values) >= 2 else [values[0]] * 3
            summary[key] = {"n": len(values), "median": statistics.median(values), "q1": quartiles[0],
                            "q3": quartiles[2], "min": values[0], "max": values[-1]}
    result = {"collected_at": datetime.now().astimezone().isoformat(), "records": records, "summary": summary}
    with open(args.out, "w", encoding="utf-8") as file:
        json.dump(result, file, ensure_ascii=False, indent=2)
    print(json.dumps(summary, indent=2))
    print("records:", len(records), "log_unavailable:", sum(1 for r in records if r.get("log_unavailable")))


if __name__ == "__main__":
    main()
