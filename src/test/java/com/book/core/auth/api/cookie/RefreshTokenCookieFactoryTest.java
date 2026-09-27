package com.book.core.auth.api.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieFactoryTest {
    private static final Duration MAX_AGE = Duration.ofDays(3);

    @Test
    void local_설정에서는_Secure가_false인_Refresh_Cookie를_생성한다() {
        final ResponseCookie cookie = new RefreshTokenCookieFactory(new AuthCookieProperties(false)).create("refresh-token", MAX_AGE);

        assertCookiePolicy(cookie);
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    void production_설정에서는_Secure가_true인_Refresh_Cookie를_생성한다() {
        final ResponseCookie cookie = new RefreshTokenCookieFactory(new AuthCookieProperties(true)).create("refresh-token", MAX_AGE);

        assertCookiePolicy(cookie);
        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    void maxAge는_전달받은_값을_그대로_사용한다() {
        final ResponseCookie cookie = new RefreshTokenCookieFactory(new AuthCookieProperties(false)).create("refresh-token", MAX_AGE);

        assertThat(cookie.getMaxAge()).isEqualTo(MAX_AGE);
    }

    private void assertCookiePolicy(final ResponseCookie cookie) {
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }
}
