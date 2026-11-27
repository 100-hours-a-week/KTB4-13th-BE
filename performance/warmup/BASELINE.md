# 기동·웜업 측정 전 기준선 조사 — Issue #614

조사일: 2026-11-27. 대상 커밋: `main`의 `5436adf`. 이 문서는 운영 환경에 접근하지 않고 읽기 전용으로 조회한 결과만 담습니다.
아직 측정하지 않은 항목은 "미측정"으로 표시하고 그 사유를 적었습니다.

## 1. CI 단계별 소요시간

수집 스크립트: [scripts/collect_ci_timings.py](scripts/collect_ci_timings.py)
원시 결과: [results/ci/ci-timings.json](results/ci/ci-timings.json)

성공한 `ci.yml` 실행만 대상으로 GitHub Actions job·step 시작·종료 시각을 집계했습니다(단위: 초).

| Job / Step | n | 중앙값 | Q1–Q3 | 최소–최대 | 표본 기간 |
| --- | ---: | ---: | --- | --- | --- |
| PR `Gradle check` job 전체 | 50 | 353.5 | 327.8–372.8 | 277–507 | 2026-11-14 ~ 11-26 |
| └ `Run Gradle check` step | 50 | 338.0 | 315–355 | 264–431 | 위와 동일 |
| └ `Set up Java`(Gradle 캐시 복원 포함) | 50 | 3.0 | 1–4.3 | 0–7 | 위와 동일 |
| main `Publish backend image` job 전체 | 7 | 94.0 | 87–101 | 87–105 | 2026-11-20 |
| └ `Build and push Docker image` step | 7 | 85.0 | 71–91 | 71–97 | 위와 동일 |

### 1.1 Gradle 태스크 분해

원시 결과: [results/ci/gradle-profile-tasks.json](results/ci/gradle-profile-tasks.json)

CI가 이미 업로드하고 있는 `--profile` 리포트에서 최근 성공한 PR 실행 5건을 읽었습니다.

| 실행 | 전체 | `integrationTest` | `test` | `compileJava` | `compileTestJava` | `bootJar` |
| --- | --- | --- | --- | --- | --- | --- |
| 44849300353 | 5m41.85s | 3m43.82s | 1m13.48s | 13.541s | 7.237s | 0.898s |
| 44993909931 | 6m3.21s | 4m5.04s | 1m17.75s | 14.294s | 7.518s | 1.293s |
| 45021416705 | 4m42.56s | 2m47.12s | 54.808s | 15.905s | 7.046s | 2.482s |
| 45417636594 | 5m52.18s | 3m53.50s | 1m15.45s | 13.716s | 7.955s | 1.199s |
| 45835850803 | 6m5.89s | 3m47.85s | 1m2.73s | 20.080s | 9.224s | 2.955s |

- 표본 5건 모두에서 PR CI 시간의 대부분을 `integrationTest`가 차지했습니다.
- `bootJar`는 3초 이내였습니다.
- 이 결과는 이미지 빌드나 AOT와는 관계가 없는 병목입니다. 이번 Issue의 범위가 아니므로 기록만 해 둡니다.

## 2. 이미지 빌드·Push 구간 분해 (main push)

수집 스크립트: [scripts/collect_image_publish_timings.py](scripts/collect_image_publish_timings.py)
원시 결과: [results/ci/image-publish-timings.json](results/ci/image-publish-timings.json)

`Publish backend image` job 로그의 BuildKit 단계 시각을 기준으로 구간을 나눴습니다. 표본은 n=40이고 기간은 2026-11-11 ~ 11-20이며, 단위는 초입니다.

| 구간 | 중앙값 | Q1–Q3 | 최소–최대 |
| --- | ---: | --- | --- |
| 컨테이너 안 `./gradlew clean bootJar` 단계 | 60.35 | 56.7–67.8 | 50.0–88.9 |
| └ Gradle 배포판 다운로드(Gradle 시작 전) | 2.76 | 2.4–3.1 | 1.9–5.3 |
| 이미지 레이어 export | 2.77 | 2.1–3.0 | 0.0–3.5 |
| ECR Push | 8.86 | 8.1–10.5 | 7.1–11.4 |
| compose build 시작 ~ Push 완료 | 85.53 | 79.3–90.4 | 71.0–109.5 |

**관찰한 사실**
- 상세히 확인한 로그 1건(run 44691582152)에는 `CACHED` 단계가 하나도 없었습니다. 이 실행에서는 Docker 레이어 캐시를 쓰지 않았다는 뜻입니다.
- 같은 로그에서 Gradle 배포판을 매번 새로 내려받았습니다.
- 컨테이너 안에서 `bootJar` 단계는 중앙값 60초가 걸렸습니다. 반면 Gradle 캐시를 쓰는 PR CI에서는 `compileJava`가 14–20초, `bootJar`가 3초 이내였습니다.

**해석(가설, 미검증)**
- 두 환경의 차이는 의존성 다운로드와 캐시 부재 때문일 가능성이 있습니다.
- 다만 두 환경은 러너, 실행한 태스크 구성, 데몬 사용 여부가 서로 다르므로 원인을 단정할 수 없습니다.
- 원인은 5단계에서 로컬 Docker 빌드로 캐시 없음, 소스 변경, 의존성 변경 조건을 나눠 검증합니다(계획의 H8).

**규모 비교**
- 이미지 빌드+Push 전체(중앙값 약 86초)는 운영 배포의 Backend ECS 롤아웃(3절, 7–15분)보다 한 자릿수 작은 규모입니다.

## 3. 운영 배포의 Backend ECS 롤아웃

원시 결과: [results/ci/prod-backend-ecs-rollout.json](results/ci/prod-backend-ecs-rollout.json)

`KTB4-13th-CLOUD`의 `cd.yml` 로그에서 다음 두 시각 사이를 쟀습니다.
- 시작: "새 task definition을 등록했습니다(prod-app-task)"
- 종료: "backend ECS 배포가 안정 상태에 도달했습니다"

| 실행 | 등록 시각(UTC) | 소요(초) |
| --- | --- | ---: |
| 43168531920 | 2026-11-12 06:30:56 | 919 |
| 43127161894 | 2026-11-12 00:26:29 | 894 |
| 39723607011 | 2026-10-22 04:47:54 | 459 |
| 39597464000 | 2026-10-21 12:18:43 | 444 |
| 39566513629 | 2026-10-21 08:12:36 | 464 |

**이 구간에 포함되는 것**
- 새 태스크의 이미지 pull, 컨테이너 기동, JVM·Spring 기동, ALB 헬스체크 통과
- 기존 태스크의 연결 drain과 종료
- 스크립트의 판정 기준은 PRIMARY COMPLETED이고 running == desired입니다.

**아직 모르는 것**
- 2026-10월 표본(약 7.5분)과 2026-11월 표본(약 15분)의 차이가 생긴 원인은 확인하지 못했습니다.
- 위 구간 중 JVM 기동과 웜업이 차지하는 비중은 **미측정**입니다.
  - 사유: 로컬 AWS 자격 증명이 유효하지 않아(`InvalidClientTokenId`) 다음 정보를 조회할 수 없었습니다.
    - ECS `describe-tasks`의 `pullStartedAt`, `pullStoppedAt`, `startedAt`
    - 서비스 이벤트, 타깃 그룹 설정, CloudWatch 기동 로그
  - 따라서 계획의 H4("배포 총시간에서 JVM 기동이 차지하는 비중은 작다")는 아직 판정할 수 없습니다.

**Dev 배포 상태**
- Dev(`cd-dev.yml`, ECS)는 2026-11-20 이후 시도가 모두 failure 또는 cancelled였습니다.
- 같은 시기 Dev Backend는 EKS(Argo CD 수동 동기화)로 옮겨졌습니다(Cloud #68/PR #70).
- 실패 원인은 이 Issue의 범위가 아니므로 확인하지 않았습니다.

## 4. 런타임 자원과 아키텍처

| 항목 | 값 | 출처 |
| --- | --- | --- |
| 운영 인스턴스 | AWS t3.small (x86_64, 2 vCPU 버스트형, 2 GiB) | 사용자 확인 |
| 운영 서비스 | ECS `prod-app-service`, desiredCount=2 | Cloud `tests/cd/README.md`(2026-10-27 조회) |
| 운영 태스크 CPU·메모리 한도 | **미측정**(AWS 조회 권한 없음) | — |
| Dev Backend | EKS requests 250m/768Mi, limits 500m/896Mi, replica 2 | Cloud `gitops/environments/dev/backend.values.yaml` |
| JVM 옵션 | `-Duser.timezone=Asia/Seoul -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0` | `Dockerfile` ENTRYPOINT |
| 실행 이미지 베이스 | `eclipse-temurin:25-jre`, CI 빌드 시점 digest `sha256:945247f9…` | 2절 로그(run 44691582152) |
| 로컬 실험 장비 | Apple M1(arm64), 8코어, 8 GiB, OpenJDK 25.0.2 | `uname`, `sysctl` |

## 5. 이미지 크기와 레이어

**미측정**입니다. 조사 시점에 로컬 Docker가 준비되지 않았고, ECR `describe-images`를 조회할 AWS 권한도 없었습니다.
2단계에서 로컬 Docker 환경을 구성한 뒤 `docker image inspect`와 `docker history`로 측정합니다.

## 6. 다음 단계에 반영할 점

1. **CI 비용 구조.** 이미지 빌드+Push(약 86초)는 PR CI(약 6분)와 운영 ECS 롤아웃(7–15분)에 비해 작습니다.
   - Docker 빌드 최적화의 기대 효과는 최대 수십 초 규모로 제한됩니다. 5단계 우선순위를 판단할 때 이 점을 고려합니다.
   - CI 시간의 실제 지배 항목은 `integrationTest`이지만, 이번 범위 밖입니다.
2. **운영 배포 시간 분해는 하지 않습니다.** 운영 AWS 조회 없이 로컬 모니터링으로 진행하기로 결정했습니다(2026-11-27).
   - 기동 시간(컨테이너 시작부터 Ready까지)은 로컬에서 측정합니다.
   - 그 값은 3절의 운영 ECS 롤아웃 시간과 나란히 비교만 합니다.
   - 운영 롤아웃 안에서 이미지 pull, 헬스체크, drain이 각각 차지하는 비중은 미측정으로 남깁니다.
   - 따라서 "기동 시간을 줄이면 운영 배포 시간이 얼마나 줄어드는가"는 결론에서 한계로 명시합니다.
3. **로컬 측정의 범위.** 로컬 측정은 JVM 기동과 웜업 구간의 상대 비교를 담당합니다.
