package com.book.core.auth.api;

import com.book.common.exception.BusinessException;
import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.auth.api.request.AuthLoginRequest;
import com.book.core.auth.api.response.AuthLoginResponse;
import com.book.core.auth.api.response.AuthReissueResponse;
import com.book.core.auth.api.spec.AuthControllerSpec;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.auth.application.usecase.AuthLoginUseCase;
import com.book.core.auth.application.usecase.AuthReissueUseCase;
import com.book.core.auth.domain.exception.AuthErrorCode;
import com.book.core.user.domain.ProviderType;
import com.book.support.web.SuccessResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.book.core.auth.application.usecase.AuthLogoutUseCase;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
class AuthController implements AuthControllerSpec {
    private final AuthLoginUseCase authLoginUseCase;
    private final AuthLogoutUseCase authLogoutUseCase;
    private final AuthReissueUseCase authReissueUseCase;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @PostMapping("/{providerType}/login")
    @Override
    public ResponseEntity<SuccessResponse<AuthLoginResponse>> login(
            @PathVariable final ProviderType providerType, @Valid @RequestBody final AuthLoginRequest request) {
        final AuthLoginResult result = authLoginUseCase.execute(request.toCommand(providerType));
        final String refreshCookie =
                refreshTokenCookieFactory.create(result.refreshToken()).toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie)
                .body(SuccessResponse.of(AuthLoginResponse.from(result)));
    }

    @PostMapping("/reissue")
    @Override
    public ResponseEntity<SuccessResponse<AuthReissueResponse>> reissue(final HttpServletRequest request) {

        final String refreshToken = extractRefreshToken(request);
        final AuthReissueResult result = authReissueUseCase.execute(refreshToken);

        final String refreshCookie =
                refreshTokenCookieFactory.create(result.refreshToken()).toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie)
                .body(SuccessResponse.of(AuthReissueResponse.from(result)));
    }

    @PostMapping("/logout")
    @Override
    public ResponseEntity<Void> logout(@AuthenticationPrincipal final Jwt jwt) {
        final Long userId = Long.valueOf(jwt.getSubject());

        authLogoutUseCase.execute(userId);

        final String expiredRefreshCookie =
            refreshTokenCookieFactory.expire().toString();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie)
            .build();
    }


    private String extractRefreshToken(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        for (final Cookie cookie : cookies) {
            if (RefreshTokenCookieFactory.COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
}
