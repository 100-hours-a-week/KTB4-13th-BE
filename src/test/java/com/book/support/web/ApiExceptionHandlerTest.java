package com.book.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.common.exception.BusinessException;
import com.book.core.auth.domain.exception.AuthErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void 인증_실패를_401로_변환한다() {
        final var response = handler.handleBusiness(new BusinessException(AuthErrorCode.INVALID_ID_TOKEN));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 유효하지_않은_인가_코드를_401로_변환한다() {
        final var response = handler.handleBusiness(new BusinessException(AuthErrorCode.INVALID_AUTHORIZATION_CODE));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 외부_인증_서비스_장애를_502로_변환한다() {
        final var response = handler.handleBusiness(new BusinessException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void 외부_인증_서비스_설정_오류를_500으로_변환한다() {
        final var response =
                handler.handleBusiness(new BusinessException(AuthErrorCode.OAUTH_PROVIDER_CONFIGURATION_ERROR));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
