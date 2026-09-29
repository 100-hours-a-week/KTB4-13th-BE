package com.book.core.auth.api;

import com.book.common.config.security.CurrentUser;
import com.book.common.response.ApiResponse;
import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.auth.api.converter.AuthCommandConverter;
import com.book.core.auth.api.request.AuthLoginRequest;
import com.book.core.auth.api.response.AuthLoginResponse;
import com.book.core.auth.api.response.AuthReissueResponse;
import com.book.core.auth.api.spec.AuthControllerSpec;
import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.command.AuthReissueCommand;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.auth.application.service.AuthService;
import com.book.core.user.domain.ProviderType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
class AuthController implements AuthControllerSpec {
    private final AuthService authService;
    private final AuthCommandConverter commandConverter;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @PostMapping("/{providerType}/login")
    @Override
    public ResponseEntity<ApiResponse<AuthLoginResponse>> login(@PathVariable final ProviderType providerType,
        @Valid @RequestBody final AuthLoginRequest request) {
        final AuthLoginCommand command = commandConverter.toAuthLoginCommand(providerType, request);
        final AuthLoginResult result = authService.login(command);
        final Duration refreshMaxAge = Duration.between(Instant.now(), result.refreshExpiresAt());
        final String refreshCookie = refreshTokenCookieFactory.create(result.refreshToken(), refreshMaxAge).toString();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie)
            .body(ApiResponse.ok(new AuthLoginResponse(result.accessToken())));
    }

    @PostMapping("/reissue")
    @Override
    public ResponseEntity<ApiResponse<AuthReissueResponse>> reissue(final HttpServletRequest request) {
        final AuthReissueCommand command = commandConverter.toAuthReissueCommand(request);
        final AuthReissueResult result = authService.reissue(command);
        final Duration refreshMaxAge = Duration.between(Instant.now(), result.refreshExpiresAt());
        final String refreshCookie = refreshTokenCookieFactory.create(result.refreshToken(), refreshMaxAge).toString();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie)
            .body(ApiResponse.ok(new AuthReissueResponse(result.accessToken())));
    }

    @PostMapping("/logout")
    @Override
    public ResponseEntity<Void> logout(@CurrentUser final Long userId) {
        authService.logout(userId);
        final String expiredRefreshCookie = refreshTokenCookieFactory.expire().toString();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredRefreshCookie).build();
    }
}
