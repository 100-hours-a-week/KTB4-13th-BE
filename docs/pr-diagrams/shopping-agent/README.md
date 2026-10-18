# 챗봇·쇼핑 에이전트 PR 시퀀스

기능 코드와 분리한 PR 검토 자료입니다. 로컬 `.docs`·`CONTEXT.md` 원문은 포함하지 않습니다.

| 기능 | 원본 | PR 표시용 |
|---|---|---|
| 장바구니 일괄 변경 | [HTML](cart.html) | [흑백 PNG](cart.monochrome.png) |
| 추천 보강 | [HTML](recommendation.html) | [흑백 PNG](recommendation.monochrome.png) |
| 제안·실행 | [HTML](proposal.html) | [흑백 PNG](proposal.monochrome.png) |
| 대화방 저장 | [HTML](chat.html) | [흑백 PNG](chat.monochrome.png) |
| 쇼핑 연결 | [HTML](shopping.html) | [흑백 PNG](shopping.monochrome.png) |

Archify 2.17 계열로 생성했습니다. 각 원본 HTML은 showcase9/9, 구성 오류·경고0을 통과했습니다. visual-check는 4개 데스크톱 크기의 브라우저 검사와 밝은/어두운 화면을 별도로 기록합니다. 소스·HTML의 SHA-256은 deliver receipt에 있습니다.

흑백 SVG/PNG는 동일한 시퀀스의 색을 검정 선·흰 배경으로 정리하고 글꼴을14px로 키운 표시용 산출물입니다. 숨겨진 상호작용 선은 표시하지 않습니다. 이 색·글꼴 변환은 원본 HTML의 결정적 전달 검사와 구분하며, 최종 PNG5개를 직접 열어 가독성·겹침·화살표를 확인했습니다. PNG의 브라우저 캡처와 원본 HTML의 검증은 애플리케이션 동작 검증이 아닙니다.

한국어 본문과 달리 고정 Viewer UI는 영어입니다. 그림은 반복 저장소 호출·반환을 묶어 표현했습니다. 쇼핑 그림은 명확한 직접 지시 흐름이며 AI 선택 조합은 제안만 저장하고 사용자 확인 뒤 실행합니다. 활성화·외부 배포를 확인한 그림이 아닙니다.
