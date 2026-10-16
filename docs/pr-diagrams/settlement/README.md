# 정산 기능별 시퀀스 다이어그램

각 기능 PR은 같은 폴더의 `*.monochrome.png`를 고정 커밋 URL로 참조합니다.
흰 배경, 검은 선·화살표·텍스트이며 호출은 실선, 반환은 점선입니다.
Port·Adapter·JpaRepository 반복 호출이나 반환을 묶은 곳이 있으므로 오류·분기·트랜잭션 경계는 각 PR 본문을 함께 확인합니다.

- merchant: 가맹점과 상품 매핑 조회
- ledger: 정산 내역 저장과 대상 집계
- sources: 결제·취소·주문상품 공개 조회
- load: 결제·전체 취소·부분 취소 대상 적재
- generate: 최근 SENT 매출 구간별 수수료와 READY 생성
- transfer: 가맹점별 정산 주기 확인과 SENT 상태 전환

Archify JSON을 validate·deliver한 뒤 정확히 같은 HTML을 visual-check했습니다.
6개 모두 showcase 9/9, 오류·경고 0건입니다. 자동 브라우저 검증과 밝은/어두운 테마의 이미지 시각 확인을 각각 수행했습니다.
네 가지 데스크톱 크기(1440×900, 1600×1000, 1920×1080, 2048×1320)를 검사했습니다.
한국어 설명은 보존되며 원본 HTML의 고정 Viewer UI와 html lang은 Archify 기본 영어입니다.

흑백 SVG·PNG는 검증한 HTML의 SVG 배치·글자·화살표 의미를 보존한 파생 산출물입니다.
뷰어 전용 hit/focus 영역을 제외하고 색상만 변환했습니다. 원본 HTML의 두 테마까지 흑백으로 바꾼 것은 아닙니다.
외부 송금은 미연동이며 SENT는 실제 송금 증거가 아닙니다.
