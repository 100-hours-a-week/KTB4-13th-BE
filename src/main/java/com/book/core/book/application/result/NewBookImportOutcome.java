package com.book.core.book.application.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum NewBookImportOutcome implements NewBookDetailLookup {
    ALREADY_REGISTERED("이미 등록되어 상세 조회를 건너뛰었습니다."),
    BUDGET_EXHAUSTED("공유 호출 예산을 사용할 수 없어 조회를 중단했습니다."),
    MISSING_DETAIL("상세 정보 또는 등록에 필요한 값이 없습니다."),
    REGISTERED("신규 도서를 등록했습니다."),
    ALREADY_REGISTERED_AFTER_LOOKUP("상세 조회 후 같은 ISBN의 등록을 확인했습니다."),
    STORAGE_REJECTED("저장 제약을 충족하지 못했습니다."),
    RATE_LIMITED("외부 API가 호출을 제한해 조회를 중단했습니다."),
    LOOKUP_FAILED("외부 API 상세 조회에 실패했습니다.");

    private final String description;
}
