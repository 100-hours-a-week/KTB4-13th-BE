#!/usr/bin/env bash

set -Eeuo pipefail

if [ "$#" -ne 8 ]; then
  echo "사용법: deploy_backend.sh <deploy-dir> <compose-file> <service> <image-uri> <release-sha> <aws-region> <ecr-registry> <health-url>" >&2
  exit 2
fi

deploy_dir=$1
compose_file=$2
service_name=$3
image_uri=$4
release_sha=$5
aws_region=$6
ecr_registry=$7
health_url=$8

env_file="$deploy_dir/.env.production"
current_file="$deploy_dir/current-release.env"
previous_file="$deploy_dir/previous-release.env"
candidate_file="$deploy_dir/candidate-release.env"

for command in aws docker curl grep sed seq tail cp mv sleep flock stat; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "필수 명령을 찾을 수 없습니다: $command" >&2
    exit 1
  fi
done

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose 플러그인을 사용할 수 없습니다." >&2
  exit 1
fi

cd "$deploy_dir"

deploy_dir_owner=$(stat -c '%u' .)
deploy_dir_mode=$(stat -c '%a' .)

if [ "$deploy_dir_owner" -ne 0 ] || (( (8#$deploy_dir_mode & 8#022) != 0 )); then
  echo "배포 디렉터리는 root 소유이고 group/other 쓰기 권한이 없어야 합니다: $deploy_dir" >&2
  exit 1
fi

exec 9>"$deploy_dir/.backend-deploy.lock"

if ! flock -n 9; then
  echo "다른 Backend 배포가 진행 중입니다." >&2
  exit 1
fi

if [ ! -f "$compose_file" ]; then
  echo "Compose 파일을 찾을 수 없습니다: $deploy_dir/$compose_file" >&2
  exit 1
fi

if [ ! -f "$env_file" ]; then
  echo "운영 환경변수 파일을 찾을 수 없습니다: $env_file" >&2
  exit 1
fi

read_release_file() {
  local file=$1
  release_file_image=$(sed -n 's/^BACKEND_IMAGE=//p' "$file" | tail -n 1)
  release_file_sha=$(sed -n 's/^RELEASE_SHA=//p' "$file" | tail -n 1)

  if [ -z "$release_file_image" ] || [ -z "$release_file_sha" ]; then
    echo "릴리스 파일 형식이 올바르지 않습니다: $file" >&2
    return 1
  fi
}

run_compose() {
  local target_image=$1
  local target_sha=$2
  shift 2

  BACKEND_IMAGE="$target_image" RELEASE_SHA="$target_sha" \
    docker compose --env-file "$env_file" -f "$compose_file" "$@"
}

health_is_ready() {
  local body
  body=$(curl --fail --silent --show-error --max-time 5 "$health_url") || return 1

  grep -Eq '"success"[[:space:]]*:[[:space:]]*true' <<< "$body"
}

wait_for_health() {
  local attempt

  for attempt in $(seq 1 30); do
    if health_is_ready; then
      echo "Backend 상태 검사가 통과했습니다."
      return 0
    fi
    sleep 5
  done

  echo "Backend 상태 검사가 제한 시간 안에 통과하지 못했습니다." >&2
  return 1
}

verify_running_image() {
  local container_id
  local running_image

  container_id=$(run_compose "$image_uri" "$release_sha" ps -q "$service_name")

  if [ -z "$container_id" ]; then
    echo "Backend 컨테이너를 찾을 수 없습니다." >&2
    return 1
  fi

  running_image=$(docker inspect --format '{{.Config.Image}}' "$container_id")

  if [ "$running_image" != "$image_uri" ]; then
    echo "실행 중인 이미지가 배포 대상과 다릅니다." >&2
    echo "예상: $image_uri" >&2
    echo "실제: $running_image" >&2
    return 1
  fi
}

rollback() {
  if [ ! -f "$previous_file" ]; then
    echo "이전 릴리스 정보가 없어 자동 Rollback을 실행할 수 없습니다." >&2
    return 1
  fi

  read_release_file "$previous_file"
  echo "이전 Backend 이미지로 Rollback합니다: $release_file_sha"
  run_compose "$release_file_image" "$release_file_sha" pull "$service_name"
  run_compose "$release_file_image" "$release_file_sha" up -d --no-deps "$service_name"
  wait_for_health
}

aws ecr get-login-password --region "$aws_region" |
  docker login --username AWS --password-stdin "$ecr_registry"

if [ -f "$current_file" ]; then
  cp "$current_file" "$previous_file"
fi

umask 077
trap 'rm -f -- "$candidate_file"' EXIT
printf 'BACKEND_IMAGE=%s\nRELEASE_SHA=%s\n' "$image_uri" "$release_sha" > "$candidate_file"

run_compose "$image_uri" "$release_sha" config --quiet
run_compose "$image_uri" "$release_sha" pull "$service_name"

if ! run_compose "$image_uri" "$release_sha" up -d --no-deps "$service_name"; then
  echo "Backend 컨테이너 교체에 실패했습니다." >&2
  rollback || true
  exit 1
fi

if ! verify_running_image; then
  rollback || true
  exit 1
fi

if ! wait_for_health; then
  rollback || true
  exit 1
fi

mv "$candidate_file" "$current_file"
echo "Backend 배포가 완료됐습니다: $release_sha"
