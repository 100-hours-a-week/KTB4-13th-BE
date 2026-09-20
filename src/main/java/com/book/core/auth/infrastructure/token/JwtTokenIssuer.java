package com.book.core.auth.infrastructure.token;

import com.book.common.exception.BusinessException;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.domain.exception.AuthErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer implements TokenIssuer {
    static final String TOKEN_TYPE_CLAIM = "tokenType";
    static final String ACCESS_TOKEN_TYPE = "ACCESS";
    static final String REFRESH_TOKEN_TYPE = "REFRESH";
    private static final JwsHeader JWT_HEADER =
            JwsHeader.with(MacAlgorithm.HS512).type("JWT").build();

    private final JwtEncoder jwtEncoder;
    private final TokenProperties properties;
    private final Clock clock;

    public JwtTokenIssuer(
            @Qualifier(JwtTokenConfiguration.TOKEN_JWT_ENCODER) final JwtEncoder jwtEncoder,
            final TokenProperties properties,
            @Qualifier(JwtTokenConfiguration.TOKEN_CLOCK) final Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedTokens issue(final Long userId) {
        final Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        final Instant refreshExpiresAt = issuedAt.plus(properties.refreshTokenExpiration());
        try {
            final String accessToken =
                    issueToken(userId, issuedAt, properties.accessTokenExpiration(), ACCESS_TOKEN_TYPE);
            final String refreshToken = issueToken(userId, issuedAt, refreshExpiresAt, REFRESH_TOKEN_TYPE);
            return new IssuedTokens(accessToken, refreshToken, refreshExpiresAt);
        } catch (final JwtEncodingException exception) {
            throw new BusinessException(AuthErrorCode.TOKEN_ISSUE_FAILURE, exception);
        }
    }

    private String issueToken(
            final Long userId, final Instant issuedAt, final Duration expiration, final String tokenType) {
        return issueToken(userId, issuedAt, issuedAt.plus(expiration), tokenType);
    }

    private String issueToken(
            final Long userId, final Instant issuedAt, final Instant expiresAt, final String tokenType) {
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JWT_HEADER, claims)).getTokenValue();
    }
}
