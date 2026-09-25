package com.book.core.auth.api.cookie;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {
    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Lax";
    private static final Duration MAX_AGE = Duration.ofDays(7);

    private final AuthCookieProperties properties;

    public RefreshTokenCookieFactory(final AuthCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(final String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(MAX_AGE)
                .build();
    }

    public ResponseCookie expire() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(SAME_SITE)
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
