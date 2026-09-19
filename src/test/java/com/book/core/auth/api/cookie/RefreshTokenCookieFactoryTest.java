package com.book.core.auth.api.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieFactoryTest {
    @Test
    void local_설정에서는_Secure가_false인_Refresh_Cookie를_생성한다() {
        final ResponseCookie cookie =
                new RefreshTokenCookieFactory(new AuthCookieProperties(false)).create("refresh-token");

        assertCookiePolicy(cookie);
        assertThat(cookie.isSecure()).isFalse();
    }

    @Test
    void production_설정에서는_Secure가_true인_Refresh_Cookie를_생성한다() {
        final ResponseCookie cookie =
                new RefreshTokenCookieFactory(new AuthCookieProperties(true)).create("refresh-token");

        assertCookiePolicy(cookie);
        assertThat(cookie.isSecure()).isTrue();
    }

    private void assertCookiePolicy(final ResponseCookie cookie) {
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofSeconds(604800));
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }
}
