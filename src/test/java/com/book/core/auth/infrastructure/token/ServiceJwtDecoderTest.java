package com.book.core.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.auth.application.port.IssuedTokens;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class ServiceJwtDecoderTest {
    private static final Long USER_ID = 42L;
    private static final Instant NOW = Instant.parse("2030-01-02T03:04:05Z");
    private static final byte[] SECRET_BYTES = new byte[64];
    private static final TokenProperties PROPERTIES = new TokenProperties(
            Base64.getEncoder().encodeToString(SECRET_BYTES), Duration.ofHours(1), Duration.ofDays(7));

    private SecretKey secretKey;
    private JwtEncoder jwtEncoder;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        final JwtTokenConfiguration configuration = new JwtTokenConfiguration();
        secretKey = configuration.tokenSecretKey(PROPERTIES);
        jwtEncoder = configuration.tokenJwtEncoder(secretKey);
        jwtDecoder = configuration.serviceJwtDecoder(secretKey, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void 정상_HS512_Access_Token을_검증한다() {
        final String accessToken = issueAt(NOW).accessToken();

        final var jwt = jwtDecoder.decode(accessToken);

        assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(jwt.getClaimAsString(JwtTokenIssuer.TOKEN_TYPE_CLAIM)).isEqualTo(JwtTokenIssuer.ACCESS_TOKEN_TYPE);
    }

    @Test
    void 만료된_Access_Token을_거부한다() {
        final String expiredToken = issueAt(NOW.minus(Duration.ofHours(2))).accessToken();

        assertDecodeFails(expiredToken);
    }

    @Test
    void 다른_Secret으로_서명한_Token을_거부한다() {
        final byte[] otherSecretBytes = new byte[64];
        Arrays.fill(otherSecretBytes, (byte) 1);
        final SecretKey otherSecretKey = new SecretKeySpec(otherSecretBytes, "HmacSHA512");
        final JwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(otherSecretKey)
                .algorithm(MacAlgorithm.HS512)
                .build();
        final String token = issue(
                otherEncoder, MacAlgorithm.HS512, NOW, JwtTokenIssuer.ACCESS_TOKEN_TYPE, true, USER_ID.toString());

        assertDecodeFails(token);
    }

    @Test
    void malformed_Token을_거부한다() {
        assertDecodeFails("not-a-jwt");
    }

    @Test
    void HS512가_아닌_Token을_거부한다() {
        final SecretKey hs256Key = new SecretKeySpec(SECRET_BYTES, "HmacSHA256");
        final JwtEncoder hs256Encoder = NimbusJwtEncoder.withSecretKey(hs256Key)
                .algorithm(MacAlgorithm.HS256)
                .build();
        final String token = issue(
                hs256Encoder, MacAlgorithm.HS256, NOW, JwtTokenIssuer.ACCESS_TOKEN_TYPE, true, USER_ID.toString());

        assertDecodeFails(token);
    }

    @Test
    void exp가_없는_Token을_거부한다() {
        final String token =
                issue(jwtEncoder, MacAlgorithm.HS512, NOW, JwtTokenIssuer.ACCESS_TOKEN_TYPE, false, USER_ID.toString());

        assertDecodeFails(token);
    }

    private IssuedTokens issueAt(final Instant issuedAt) {
        final var issuer = new JwtTokenIssuer(jwtEncoder, PROPERTIES, Clock.fixed(issuedAt, ZoneOffset.UTC));
        return issuer.issue(USER_ID);
    }

    private String issue(
            final JwtEncoder encoder,
            final MacAlgorithm algorithm,
            final Instant issuedAt,
            final String tokenType,
            final boolean includeExpiration,
            final String subject) {
        final JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .subject(subject)
                .claim(JwtTokenIssuer.TOKEN_TYPE_CLAIM, tokenType);
        if (includeExpiration) {
            claims.expiresAt(issuedAt.plus(Duration.ofHours(1)));
        }
        return encoder.encode(
                        JwtEncoderParameters.from(JwsHeader.with(algorithm).build(), claims.build()))
                .getTokenValue();
    }

    private void assertDecodeFails(final String token) {
        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
