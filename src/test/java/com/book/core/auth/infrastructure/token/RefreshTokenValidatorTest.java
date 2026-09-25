package com.book.core.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

class RefreshTokenValidatorTest {
    private static final Instant NOW = Instant.parse("2030-01-02T03:04:05Z");
    private final RefreshTokenValidator validator = new RefreshTokenValidator();

    @Test
    void REFRESH_tokenType과_양수_Long_subject를_허용한다() {
        assertThat(validator
                        .validate(jwt("42", JwtTokenIssuer.REFRESH_TOKEN_TYPE))
                        .hasErrors())
                .isFalse();
    }

    @Test
    void ACCESS_tokenType을_거부한다() {
        assertThat(validator
                        .validate(jwt("42", JwtTokenIssuer.ACCESS_TOKEN_TYPE))
                        .hasErrors())
                .isTrue();
    }

    @Test
    void tokenType이_없으면_거부한다() {
        assertThat(validator.validate(jwt("42", null)).hasErrors()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "", " "})
    void 알_수_없는_tokenType을_거부한다(final String tokenType) {
        assertThat(validator.validate(jwt("42", tokenType)).hasErrors()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "not-a-number", "0", "-1"})
    void 유효한_양수_Long이_아닌_subject를_거부한다(final String subject) {
        assertThat(validator
                        .validate(jwt(subject, JwtTokenIssuer.REFRESH_TOKEN_TYPE))
                        .hasErrors())
                .isTrue();
    }

    private Jwt jwt(final String subject, final String tokenType) {
        final Jwt.Builder jwt =
                Jwt.withTokenValue("token").header("alg", "HS512").issuedAt(NOW).expiresAt(NOW.plusSeconds(3600));

        if (subject != null) {
            jwt.subject(subject);
        }
        if (tokenType != null) {
            jwt.claim(JwtTokenIssuer.TOKEN_TYPE_CLAIM, tokenType);
        }

        return jwt.build();
    }
}
