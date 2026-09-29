package com.book.core.auth.api.converter;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.auth.api.request.AuthLoginRequest;
import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.command.AuthReissueCommand;
import com.book.core.user.domain.ProviderType;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthCommandConverter {
    public AuthLoginCommand toAuthLoginCommand(final ProviderType providerType, final AuthLoginRequest request) {
        return new AuthLoginCommand(providerType, request.authorizationCode(), request.codeVerifier(), request.nonce());
    }

    public AuthReissueCommand toAuthReissueCommand(final HttpServletRequest request) {
        return new AuthReissueCommand(extractRefreshToken(request));
    }

    private String extractRefreshToken(final HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (final Cookie cookie : cookies) {
                if (RefreshTokenCookieFactory.COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
