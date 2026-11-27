#!/usr/bin/env python3
"""CI workflow 실행의 job·step 소요시간을 GitHub API로 수집합니다.

읽기 전용 조회입니다. `gh` CLI 인증이 필요합니다.

사용법:
  python3 collect_ci_timings.py --limit 40 --out ../results/ci/ci-timings.json
"""
import argparse
import json
import statistics
import subprocess
from datetime import datetime

REPO = "100-hours-a-week/KTB4-13th-BE"


def gh_json(*args):
    output = subprocess.run(["gh", *args], check=True, capture_output=True, text=True).stdout
    return json.loads(output)


def seconds(start, end):
    if not start or not end:
        return None
    parse = lambda value: datetime.fromisoformat(value.replace("Z", "+00:00"))
    return (parse(end) - parse(start)).total_seconds()


def collect(limit):
    runs = gh_json(
        "run", "list", "-R", REPO, "--workflow", "ci.yml", "--limit", str(limit),
        "--json", "databaseId,event,conclusion,createdAt,headBranch,headSha",
    )
    records = []
    for run in runs:
        if run["conclusion"] != "success":
            continue
        jobs = gh_json("api", f"repos/{REPO}/actions/runs/{run['databaseId']}/jobs?per_page=100")["jobs"]
        for job in jobs:
            if job["conclusion"] != "success":
                continue
            records.append({
                "run_id": run["databaseId"],
                "event": run["event"],
                "created_at": run["createdAt"],
                "head_sha": run["headSha"],
                "job": job["name"],
                "runner": job.get("runner_name"),
                "job_seconds": seconds(job["started_at"], job["completed_at"]),
                "steps": [
                    {"name": step["name"], "seconds": seconds(step.get("started_at"), step.get("completed_at"))}
                    for step in job.get("steps", [])
                    if step.get("conclusion") == "success"
                ],
            })
    return records


def summarize(records):
    summary = {}
    for record in records:
        job = summary.setdefault(record["job"], {"job_seconds": [], "steps": {}})
        job["job_seconds"].append(record["job_seconds"])
        for step in record["steps"]:
            if step["seconds"] is not None:
                job["steps"].setdefault(step["name"], []).append(step["seconds"])

    def stats(values):
        values = sorted(value for value in values if value is not None)
        if not values:
            return None
        quartiles = statistics.quantiles(values, n=4) if len(values) >= 2 else [values[0]] * 3
        return {
            "n": len(values),
            "median": statistics.median(values),
            "q1": quartiles[0],
            "q3": quartiles[2],
            "min": values[0],
            "max": values[-1],
        }

    return {
        name: {
            "job_seconds": stats(job["job_seconds"]),
            "steps": {step: stats(values) for step, values in job["steps"].items()},
        }
        for name, job in summary.items()
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int, default=40)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    records = collect(args.limit)
    result = {
        "collected_at": datetime.now().astimezone().isoformat(),
        "repository": REPO,
        "workflow": "ci.yml",
        "run_limit": args.limit,
        "records": records,
        "summary": summarize(records),
    }
    with open(args.out, "w", encoding="utf-8") as file:
        json.dump(result, file, ensure_ascii=False, indent=2)
    print(json.dumps(result["summary"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
