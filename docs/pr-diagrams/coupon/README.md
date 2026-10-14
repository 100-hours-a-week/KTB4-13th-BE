# 쿠폰 PR 시퀀스 다이어그램

Archify로 생성한 각 PR의 코드 흐름입니다. 컬러 PNG는 뷰어의 전체 다이어그램 내보내기로 생성했습니다. PR에는 전체 SVG 내보내기에서 색상만 바꾼 흰색 배경/검은 선 PNG를 사용합니다.
정상 흐름을 중심으로 표현하며, 반복되는 영속 계층은 Port / Adapter / JPA로 묶어 표시합니다.
오류 조건, 미구현 범위와 트랜잭션 경계는 해당 PR 본문에서 함께 설명합니다.

| PR | 기준 코드 커밋 | 그림 |
| --- | --- | --- |
| #275 | `f1c8eaff1ebb8b53c6d91b416d9a457f7f8a16a4` | [흑백 PNG](pr-275.monochrome.png) |
| #276 | `7d108160247e57df82d075cc8e8735d9ad71788a` | [흑백 PNG](pr-276.monochrome.png) |
| #277 | `b356b31bd3b66163950f15fb22a1b43d27d4b8bf` | [흑백 PNG](pr-277.monochrome.png) |
| #278 | `28db1b28daa36397a573dbfa19f5db4a12078918` | [흑백 PNG](pr-278.monochrome.png) |
| #279 | `1fb74e52dc3c98eaa10f4cbbc5738a157c70a800` | [흑백 PNG](pr-279.monochrome.png) |
| #280 | `6f4a14c007e2f08e3a45fd1ed7546b549a10b3b2` | [흑백 PNG](pr-280.monochrome.png) |
| #287 | `50f615ba2543b4bceb21dbd7702bc7d297fa60a6` | [흑백 PNG](pr-287.monochrome.png) |

각 JSON은 재생성용 명세이며 receipts.json은 명세와 HTML의 검증 해시, PNG 해시를 기록합니다.
7개 모두 showcase 9/9, 오류/경고 0건과 브라우저 검증을 통과했으며 PNG를 시각적으로 확인했습니다.

흑백 SVG/PNG는 호출 관계와 텍스트가 원본과 일치하며, PNG 전체 픽셀의 무채색 및 흰색 배경을 검사했습니다. monochrome-receipts.json에 검증과 파일 해시를 기록합니다.

## 보유 쿠폰 기반 분리

#290은 #276과 #277 사이에 추가한 선행 PR입니다. [흑백 PNG](pr-290.monochrome.png)는 보유 모델 생성과 저장/조회 계약을 보여주며 발급 업무는 #277에 남습니다.
