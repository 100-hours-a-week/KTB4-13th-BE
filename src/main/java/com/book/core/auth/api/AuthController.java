package com.book.core.auth.api;

import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.auth.api.request.AuthLoginRequest;
import com.book.core.auth.api.response.AuthLoginResponse;
import com.book.core.auth.api.spec.AuthControllerSpec;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.usecase.AuthLoginUseCase;
import com.book.core.user.domain.ProviderType;
import com.book.support.web.SuccessResponse;
import jakarta.validation.Valid;
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
    private final AuthLoginUseCase authLoginUseCase;
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
}
