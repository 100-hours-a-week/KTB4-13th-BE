package com.book.core.auth.infrastructure.token;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class JwtRefreshTokenVerifier implements RefreshTokenVerifier {
    private final JwtDecoder jwtDecoder;
    private final CheckUserRestrictionUseCase checkUserRestrictionUseCase;

    public JwtRefreshTokenVerifier(@Qualifier(JwtTokenConfiguration.REFRESH_JWT_DECODER) final JwtDecoder jwtDecoder,
        final CheckUserRestrictionUseCase checkUserRestrictionUseCase) {
        this.jwtDecoder = jwtDecoder;
        this.checkUserRestrictionUseCase = checkUserRestrictionUseCase;
    }

    @Override
    public Long verify(final String refreshToken) {
        final Long userId;
        try {
            final Jwt jwt = jwtDecoder.decode(refreshToken);
            userId = Long.valueOf(jwt.getSubject());
        } catch (final JwtException | NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, exception);
        }
        if (checkUserRestrictionUseCase.execute(userId)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return userId;
    }
}
