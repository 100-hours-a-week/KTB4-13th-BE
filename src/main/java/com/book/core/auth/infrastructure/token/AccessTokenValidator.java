package com.book.core.auth.infrastructure.token;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

final class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error INVALID_ACCESS_TOKEN =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Access Token claims are invalid.", null);

    @Override
    public OAuth2TokenValidatorResult validate(final Jwt jwt) {
        if (!JwtTokenIssuer.ACCESS_TOKEN_TYPE.equals(jwt.getClaimAsString(JwtTokenIssuer.TOKEN_TYPE_CLAIM))) {
            return failure();
        }
        final String subject = jwt.getSubject();
        if (!StringUtils.hasText(subject)) {
            return failure();
        }
        try {
            if (Long.parseLong(subject) <= 0) {
                return failure();
            }
        } catch (final NumberFormatException exception) {
            return failure();
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult failure() {
        return OAuth2TokenValidatorResult.failure(INVALID_ACCESS_TOKEN);
    }
}
