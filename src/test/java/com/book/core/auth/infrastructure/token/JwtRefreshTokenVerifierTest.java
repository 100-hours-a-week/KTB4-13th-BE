package com.book.core.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.BusinessException;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.domain.exception.AuthErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class JwtRefreshTokenVerifierTest {
    private static final Long USER_ID = 42L;
    private static final Instant NOW = Instant.parse("2030-01-02T03:04:05Z");
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);

    private static final byte[] SECRET_BYTES = new byte[64];
    private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);

    private SecretKey secretKey;
    private JwtTokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        secretKey = new SecretKeySpec(SECRET_BYTES, "HmacSHA512");

        final TokenProperties properties =
                new TokenProperties(SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);

        final JwtEncoder jwtEncoder = new JwtTokenConfiguration().tokenJwtEncoder(secretKey);

        tokenIssuer = new JwtTokenIssuer(jwtEncoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void 유효한_Refresh_Token에서_userId를_반환한다() {
        final IssuedTokens tokens = tokenIssuer.issue(USER_ID);
        final JwtRefreshTokenVerifier verifier = verifierAt(NOW);

        final Long userId = verifier.verify(tokens.refreshToken());

        assertThat(userId).isEqualTo(USER_ID);
    }

    @Test
    void Access_Token을_Refresh_Token으로_사용하면_거부한다() {
        final IssuedTokens tokens = tokenIssuer.issue(USER_ID);
        final JwtRefreshTokenVerifier verifier = verifierAt(NOW);

        assertInvalidRefreshToken(() -> verifier.verify(tokens.accessToken()));
    }

    @Test
    void 만료된_Refresh_Token을_거부한다() {
        final IssuedTokens tokens = tokenIssuer.issue(USER_ID);

        final Instant afterExpiration = NOW.plus(REFRESH_TOKEN_EXPIRATION).plusSeconds(61);
        final JwtRefreshTokenVerifier verifier = verifierAt(afterExpiration);

        assertInvalidRefreshToken(() -> verifier.verify(tokens.refreshToken()));
    }

    @Test
    void 다른_Secret으로_서명된_Refresh_Token을_거부한다() {
        final byte[] otherSecretBytes = new byte[64];
        Arrays.fill(otherSecretBytes, (byte) 1);

        final SecretKey otherSecretKey = new SecretKeySpec(otherSecretBytes, "HmacSHA512");

        final JwtEncoder otherEncoder = new JwtTokenConfiguration().tokenJwtEncoder(otherSecretKey);

        final TokenProperties properties =
                new TokenProperties(SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);

        final JwtTokenIssuer otherIssuer =
                new JwtTokenIssuer(otherEncoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));

        final String invalidRefreshToken = otherIssuer.issue(USER_ID).refreshToken();

        final JwtRefreshTokenVerifier verifier = verifierAt(NOW);

        assertInvalidRefreshToken(() -> verifier.verify(invalidRefreshToken));
    }

    private JwtRefreshTokenVerifier verifierAt(final Instant now) {
        final JwtDecoder decoder =
                new JwtTokenConfiguration().refreshJwtDecoder(secretKey, Clock.fixed(now, ZoneOffset.UTC));

        return new JwtRefreshTokenVerifier(decoder);
    }

    private void assertInvalidRefreshToken(final Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }
}
