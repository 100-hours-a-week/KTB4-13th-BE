package com.book.common.exception;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

@Getter
@Accessors(fluent = true)
public enum ErrorCode {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "알 수 없는 오류가 발생했습니다.", LogLevel.ERROR), INVALID_REQUEST(HttpStatus.BAD_REQUEST,
        "E400", "요청 형식이 올바르지 않습니다.", LogLevel.INFO), UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증 정보가 유효하지 않습니다.",
            LogLevel.INFO), FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "올바르지 않은 접근입니다.", LogLevel.INFO), ADDRESS_LIMIT_EXCEEDED(
                HttpStatus.BAD_REQUEST, "E400", "활성 주소지는 최대 3개까지 등록할 수 있습니다.", LogLevel.INFO), DUPLICATE_ADDRESS(HttpStatus.BAD_REQUEST,
                    "E400", "동일한 주소·상세주소·별칭이 이미 등록되어 있습니다.", LogLevel.INFO), CART_ITEM_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "E8000",
                        "장바구니에 담을 수 있는 상품 수를 초과했습니다.", LogLevel.INFO), INSUFFICIENT_PRODUCT_STOCK(HttpStatus.BAD_REQUEST, "E8001",
                            "요청 수량이 현재 재고를 초과합니다.", LogLevel.INFO), INVALID_CART_ITEM_QUANTITY(HttpStatus.BAD_REQUEST, "E400",
                                "장바구니 상품 수량은 1 이상 500 이하여야 합니다.", LogLevel.INFO), CART_NOT_FOUND(HttpStatus.NOT_FOUND, "E401",
                                    "장바구니를 찾을 수 없습니다.", LogLevel.INFO), CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "E401",
                                        "장바구니 상품을 찾을 수 없습니다.", LogLevel.INFO), PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "E404",
                                            "상품을 찾을 수 없습니다.", LogLevel.INFO), PRODUCT_MISMATCH_IN_ORDER(HttpStatus.BAD_REQUEST, "E3000",
                                                "요청한 상품 정보와 일치하지 않습니다.", LogLevel.INFO), ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "E404",
                                                    "주문 상품을 찾을 수 없습니다.", LogLevel.INFO), REVIEW_HAS_NOT_ORDER(HttpStatus.BAD_REQUEST,
                                                        "E7000", "리뷰 작성 가능한 주문이 없습니다.", LogLevel.INFO), REVIEW_ALREADY_REVIEWED(
                                                            HttpStatus.BAD_REQUEST, "E7001", "이미 리뷰를 작성한 상품입니다.",
                                                            LogLevel.INFO), REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "리뷰를 찾을 수 없습니다.",
                                                                LogLevel.INFO), STORAGE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR,
                                                                    "STORAGE_FAILURE", "저장소 작업을 완료할 수 없습니다.",
                                                                    LogLevel.ERROR), INVALID_USER_ID(HttpStatus.BAD_REQUEST,
                                                                        "INVALID_USER_ID", "회원 ID는 양수여야 합니다.",
                                                                        LogLevel.INFO), INVALID_NICKNAME(HttpStatus.BAD_REQUEST,
                                                                            "INVALID_NICKNAME", "닉네임은 앞뒤 공백을 제외하고 2자 이상 20자 이하여야 합니다.",
                                                                            LogLevel.INFO), INVALID_USER_PROVIDER_ID(HttpStatus.BAD_REQUEST,
                                                                                "INVALID_USER_PROVIDER_ID", "회원 연동 ID는 양수여야 합니다.",
                                                                                LogLevel.INFO), INVALID_PROVIDER_TYPE(
                                                                                    HttpStatus.BAD_REQUEST, "INVALID_PROVIDER_TYPE",
                                                                                    "소셜 로그인 제공자 유형이 필요합니다.",
                                                                                    LogLevel.INFO), INVALID_PROVIDER_USER_ID(
                                                                                        HttpStatus.BAD_REQUEST, "INVALID_PROVIDER_USER_ID",
                                                                                        "소셜 로그인 사용자 ID는 1자 이상 255자 이하여야 합니다.",
                                                                                        LogLevel.INFO), INVALID_PROVIDER_EMAIL(
                                                                                            HttpStatus.BAD_REQUEST,
                                                                                            "INVALID_PROVIDER_EMAIL",
                                                                                            "소셜 로그인 이메일은 254자 이하여야 합니다.",
                                                                                            LogLevel.INFO), NICKNAME_CONFLICT(
                                                                                                HttpStatus.CONFLICT, "NICKNAME_CONFLICT",
                                                                                                "동일한 활성 닉네임이 이미 존재합니다.",
                                                                                                LogLevel.INFO), NICKNAME_GENERATION_FAILED(
                                                                                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                                                                                    "NICKNAME_GENERATION_FAILED",
                                                                                                    "닉네임을 생성하지 못했습니다.", LogLevel.ERROR),
    // spotless:off
    PROVIDER_IDENTITY_CONFLICT(
        HttpStatus.CONFLICT,
        "PROVIDER_IDENTITY_CONFLICT",
        "동일한 활성 소셜 로그인 계정이 이미 존재합니다.",
        LogLevel.INFO),
    // spotless:on
    // spotless:off
    USER_PROVIDER_USER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "USER_PROVIDER_USER_NOT_FOUND", "회원 연결의 내부 회원을 찾을 수 없습니다.",
        LogLevel.ERROR),
    USER_PROVIDER_USER_INACTIVE(HttpStatus.INTERNAL_SERVER_ERROR, "USER_PROVIDER_USER_INACTIVE", "회원 연결의 내부 회원이 비활성 상태입니다.",
        LogLevel.ERROR),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다.", LogLevel.INFO),
    ORDER_CANNOT_BE_CANCELED(HttpStatus.CONFLICT, "ORDER_CANNOT_BE_CANCELED", "현재 상태의 주문은 취소할 수 없습니다.", LogLevel.INFO),
    INVALID_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, "INVALID_AUTHORIZATION_CODE", "인가 코드가 유효하지 않습니다.", LogLevel.INFO),
    OAUTH_TOKEN_EXCHANGE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH_TOKEN_EXCHANGE_FAILURE", "외부 인증 토큰을 발급할 수 없습니다.",
        LogLevel.ERROR),
    OAUTH_PROVIDER_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "OAUTH_PROVIDER_CONFIGURATION_ERROR",
        "외부 인증 서비스 설정이 올바르지 않습니다.", LogLevel.ERROR),
    OAUTH_PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "OAUTH_PROVIDER_UNAVAILABLE", "외부 인증 서비스를 사용할 수 없습니다.", LogLevel.ERROR),
    TOKEN_ISSUE_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "TOKEN_ISSUE_FAILURE", "인증 토큰을 발급할 수 없습니다.", LogLevel.ERROR),
    INVALID_ID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ID_TOKEN", "ID Token이 유효하지 않습니다.", LogLevel.INFO),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh Token이 유효하지 않습니다.", LogLevel.INFO),
    AI_SERVICE_UNAUTHORIZED(HttpStatus.BAD_GATEWAY, "E502", "외부 AI 서비스 인증에 실패했습니다.", LogLevel.ERROR),
    AI_SERVICE_FAILURE(HttpStatus.BAD_GATEWAY, "E502", "외부 AI 서비스 호출에 실패했습니다.", LogLevel.INFO),
    ONBOARDING_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "온보딩 질문을 찾을 수 없습니다.", LogLevel.INFO),
    ONBOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "진행 중인 온보딩이 없습니다.", LogLevel.INFO),
    ONBOARDING_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "온보딩 선택지를 찾을 수 없습니다.", LogLevel.INFO),
    ONBOARDING_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "존재하지 않는 도서가 포함되어 있습니다.", LogLevel.INFO),
    INVALID_ONBOARDING_ANSWER_SELECTION(HttpStatus.BAD_REQUEST, "E400", "온보딩 답변 선택 개수나 형식이 올바르지 않습니다.", LogLevel.INFO),
    ONBOARDING_OPTION_QUESTION_MISMATCH(HttpStatus.CONFLICT, "E409", "선택지가 해당 질문에 속하지 않습니다.", LogLevel.INFO),
    ONBOARDING_PARENT_QUESTION_NOT_ANSWERED(HttpStatus.CONFLICT, "E409", "선행 질문에 대한 답변이 필요합니다.", LogLevel.INFO),
    INVALID_ONBOARDING_BOOK_SELECTION(HttpStatus.BAD_REQUEST, "E400", "선택한 도서 목록의 형식이 올바르지 않습니다.", LogLevel.INFO),
    RECOMMENDATION_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "추천 카드를 찾을 수 없습니다.", LogLevel.INFO),
    AI_RECOMMENDATION_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "E429", "AI 추천 서비스 요청이 많아 잠시 후 다시 시도해야 합니다.", LogLevel.INFO),
    AI_RECOMMENDATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "E503", "AI 추천 서비스를 일시적으로 사용할 수 없습니다.", LogLevel.ERROR),
    AI_RECOMMENDATION_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "E504", "AI 추천 서비스 응답이 시간 내에 도착하지 않았습니다.", LogLevel.INFO),
    AI_RECOMMENDATION_SPEC_VIOLATION(HttpStatus.BAD_GATEWAY, "E502", "AI 추천 서비스가 유효하지 않은 추천 조건을 반환했습니다.", LogLevel.INFO),
    AI_RECOMMENDATION_FAILURE(HttpStatus.BAD_GATEWAY, "E502", "AI 추천 서비스 호출에 실패했습니다.", LogLevel.INFO),
    AI_FEED_INVALID_REQUEST(HttpStatus.BAD_GATEWAY, "E502", "AI 추천 피드 서비스가 요청을 거부했습니다.", LogLevel.ERROR),
    AI_FEED_CURSOR_EXPIRED(HttpStatus.GONE, "E410", "추천 피드 커서가 만료되었습니다. 처음부터 다시 조회해야 합니다.", LogLevel.INFO),
    AI_FEED_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "E504", "AI 추천 피드 서비스 응답이 시간 내에 도착하지 않았습니다.", LogLevel.INFO),
    AI_FEED_FAILURE(HttpStatus.BAD_GATEWAY, "E502", "AI 추천 피드 조회에 실패했습니다.", LogLevel.INFO);
    // spotless:on

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final LogLevel logLevel;

    ErrorCode(final HttpStatus status, final String code, final String message, final LogLevel logLevel) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.logLevel = logLevel;
    }
}
