package com.book.core.auth.infrastructure.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("auth.token")
public class TokenProperties {
    private final String secret;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public TokenProperties(final String secret, final Duration accessTokenExpiration, final Duration refreshTokenExpiration) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("auth.token.secret 설정이 필요합니다.");
        }
        if (accessTokenExpiration == null) {
            throw new IllegalStateException("auth.token.access-token-expiration 설정이 필요합니다.");
        }
        if (refreshTokenExpiration == null) {
            throw new IllegalStateException("auth.token.refresh-token-expiration 설정이 필요합니다.");
        }
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String secret() {
        return secret;
    }

    public Duration accessTokenExpiration() {
        return accessTokenExpiration;
    }

    public Duration refreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
