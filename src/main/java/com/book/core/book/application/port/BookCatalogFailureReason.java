package com.book.core.book.application.port;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum BookCatalogFailureReason {
    TEMPORARY("외부 API 응답 또는 통신 오류로 조회를 다시 시도할 수 있습니다."),
    RATE_LIMIT("외부 API의 호출 한도에 도달했습니다."),
    REJECTED("외부 API가 요청을 거부했습니다.");

    private final String description;
}
