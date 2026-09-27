package com.book.core.auth.api.cookie;

import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {
    private static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";
    private static final String SAME_SITE = "Lax";

    private final AuthCookieProperties properties;

    public RefreshTokenCookieFactory(final AuthCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(final String refreshToken, final Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken).httpOnly(true).secure(properties.secure()).sameSite(SAME_SITE)
            .path(COOKIE_PATH).maxAge(maxAge).build();
    }
}
