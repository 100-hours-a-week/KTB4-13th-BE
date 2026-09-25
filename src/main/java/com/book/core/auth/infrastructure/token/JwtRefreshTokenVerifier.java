package com.book.core.auth.infrastructure.token;

import com.book.common.exception.BusinessException;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.domain.exception.AuthErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class JwtRefreshTokenVerifier implements RefreshTokenVerifier {
    private final JwtDecoder jwtDecoder;

    public JwtRefreshTokenVerifier(@Qualifier(JwtTokenConfiguration.REFRESH_JWT_DECODER) final JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Long verify(final String refreshToken) {
        try {
            final Jwt jwt = jwtDecoder.decode(refreshToken);
            return Long.valueOf(jwt.getSubject());
        } catch (final JwtException | NumberFormatException exception) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN, exception);
        }
    }
}
