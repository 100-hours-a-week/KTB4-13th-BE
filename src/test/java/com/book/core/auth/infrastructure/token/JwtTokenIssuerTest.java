package com.book.core.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
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
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtTokenIssuerTest {
    private static final Long USER_ID = 42L;
    private static final Instant NOW = Instant.parse("2030-01-02T03:04:05Z");
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);
    private static final byte[] SECRET_BYTES = new byte[64];
    private static final String SECRET = Base64.getEncoder().encodeToString(SECRET_BYTES);

    private TokenProperties properties;
    private SecretKey secretKey;
    private JwtTokenIssuer tokenIssuer;

    @BeforeEach
    void setUp() {
        properties = new TokenProperties(SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
        secretKey = new SecretKeySpec(SECRET_BYTES, "HmacSHA512");
        final JwtEncoder jwtEncoder = new JwtTokenConfiguration().tokenJwtEncoder(secretKey);
        tokenIssuer = new JwtTokenIssuer(jwtEncoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void Access와_Refresh_Token을_HS512로_각각_발급한다() {
        final IssuedTokens tokens = tokenIssuer.issue(USER_ID);

        assertThat(tokens.accessToken()).isNotEqualTo(tokens.refreshToken());
        assertToken(tokens.accessToken(), JwtTokenIssuer.ACCESS_TOKEN_TYPE, NOW.plus(ACCESS_TOKEN_EXPIRATION));
        assertToken(tokens.refreshToken(), JwtTokenIssuer.REFRESH_TOKEN_TYPE, NOW.plus(REFRESH_TOKEN_EXPIRATION));
    }

    @Test
    void 올바른_Secret으로_signature를_검증할_수_있다() {
        final IssuedTokens tokens = tokenIssuer.issue(USER_ID);

        assertThat(decoder(secretKey).decode(tokens.accessToken())).isNotNull();
        assertThat(decoder(secretKey).decode(tokens.refreshToken())).isNotNull();
    }

    @Test
    void 다른_Secret으로_signature를_검증할_수_없다() {
        final IssuedTokens tokens = tokenIssuer.issue(USER_ID);
        final byte[] otherSecretBytes = new byte[64];
        Arrays.fill(otherSecretBytes, (byte) 1);
        final SecretKey otherSecretKey = new SecretKeySpec(otherSecretBytes, "HmacSHA512");

        assertThatThrownBy(() -> decoder(otherSecretKey).decode(tokens.accessToken()))
                .isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
    }

    @Test
    void Secret이_Base64_형식이_아니면_INTERNAL_ERROR로_실패한다() {
        final TokenProperties invalidProperties =
                new TokenProperties("not-base64!", ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);

        assertConfigurationFailure(invalidProperties, "Base64");
    }

    @Test
    void 디코딩한_Secret이_64바이트보다_짧으면_INTERNAL_ERROR로_실패한다() {
        final String shortSecret = Base64.getEncoder().encodeToString(new byte[63]);
        final TokenProperties invalidProperties =
                new TokenProperties(shortSecret, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);

        assertConfigurationFailure(invalidProperties, "64바이트");
    }

    @Test
    void JWT_encoder_오류를_INTERNAL_ERROR로_변환한다() {
        final JwtEncoder failedEncoder = mock(JwtEncoder.class);
        when(failedEncoder.encode(any(JwtEncoderParameters.class)))
                .thenThrow(new JwtEncodingException("encoding failed"));
        final JwtTokenIssuer failedIssuer =
                new JwtTokenIssuer(failedEncoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> failedIssuer.issue(USER_ID))
                .isInstanceOfSatisfying(BusinessException.class, (final var exception) -> {
                    assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.TOKEN_ISSUE_FAILURE);
                    assertThat(exception.errorCode().category()).isEqualTo(ErrorCode.Category.INTERNAL_ERROR);
                    assertThat(exception.getCause()).isInstanceOf(JwtEncodingException.class);
                });
    }

    private void assertToken(final String token, final String tokenType, final Instant expiresAt) {
        final Jwt jwt = decoder(secretKey).decode(token);
        assertThat(jwt.getHeaders().get("alg")).isEqualTo(MacAlgorithm.HS512.getName());
        assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(jwt.getClaimAsString(JwtTokenIssuer.TOKEN_TYPE_CLAIM)).isEqualTo(tokenType);
    }

    private JwtDecoder decoder(final SecretKey verificationKey) {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(verificationKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
        decoder.setJwtValidator((final Jwt jwt) -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    private void assertConfigurationFailure(final TokenProperties invalidProperties, final String causeMessage) {
        assertThatThrownBy(() -> new JwtTokenConfiguration().tokenSecretKey(invalidProperties))
                .isInstanceOfSatisfying(BusinessException.class, (final var exception) -> {
                    assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.TOKEN_ISSUE_FAILURE);
                    assertThat(exception.errorCode().category()).isEqualTo(ErrorCode.Category.INTERNAL_ERROR);
                    assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
                    assertThat(exception.getCause()).hasMessageContaining(causeMessage);
                });
    }
}
