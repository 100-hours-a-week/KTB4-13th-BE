package com.book.core.auth.infrastructure.client.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.domain.exception.AuthErrorCode;
import com.book.core.user.domain.ProviderType;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.RemoteKeySourceException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class KakaoOAuthProviderClientImplTest {
    private static final String CLIENT_ID = "test-kakao-client-id";
    private static final String KEY_ID = "test-key-id";
    private static final String PROVIDER_USER_ID = "123456789";
    private static final String EXPECTED_NONCE = "expected-nonce";
    private static final Instant NOW = Instant.now();

    private RSAKey signingKey;
    private KakaoOAuthProviderClientImpl client;

    @BeforeEach
    void setUp() throws JOSEException {
        signingKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        client = new KakaoOAuthProviderClientImpl(decoder(signingKey.toPublicJWK()));
    }

    @Test
    void 정상_RS256_ID_Token의_sub와_email을_OAuthIdentity로_반환한다() {
        final String idToken = token(signingKey, SignatureAlgorithm.RS256, PROVIDER_USER_ID, "reader@example.com");

        final var identity = client.verify(ProviderType.KAKAO, idToken, EXPECTED_NONCE);

        assertThat(identity.providerUserId()).isEqualTo(PROVIDER_USER_ID);
        assertThat(identity.providerEmail()).isEqualTo("reader@example.com");
    }

    @Test
    void email_claim이_없으면_null을_반환한다() {
        final String idToken = token(signingKey, SignatureAlgorithm.RS256, PROVIDER_USER_ID, null);

        final var identity = client.verify(ProviderType.KAKAO, idToken, EXPECTED_NONCE);

        assertThat(identity.providerEmail()).isNull();
    }

    @Test
    void 만료된_token은_UNAUTHORIZED로_변환한다() {
        final String idToken = token(
                signingKey,
                SignatureAlgorithm.RS256,
                KakaoOAuthClientConfiguration.KAKAO_ISSUER,
                CLIENT_ID,
                NOW.minusSeconds(60),
                PROVIDER_USER_ID,
                null,
                EXPECTED_NONCE);

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void issuer가_다른_token은_UNAUTHORIZED로_변환한다() {
        final String idToken = token(
                signingKey,
                SignatureAlgorithm.RS256,
                "https://invalid.example.com",
                CLIENT_ID,
                NOW.plusSeconds(300),
                PROVIDER_USER_ID,
                null,
                EXPECTED_NONCE);

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void audience가_다른_token은_UNAUTHORIZED로_변환한다() {
        final String idToken = token(
                signingKey,
                SignatureAlgorithm.RS256,
                KakaoOAuthClientConfiguration.KAKAO_ISSUER,
                "other-client-id",
                NOW.plusSeconds(300),
                PROVIDER_USER_ID,
                null,
                EXPECTED_NONCE);

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void signature가_다른_token은_UNAUTHORIZED로_변환한다() throws JOSEException {
        final RSAKey otherKey = new RSAKeyGenerator(2048).keyID(KEY_ID).generate();
        final String idToken = token(otherKey, SignatureAlgorithm.RS256, PROVIDER_USER_ID, null);

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void RS256이_아닌_token은_UNAUTHORIZED로_변환한다() {
        final String idToken = token(signingKey, SignatureAlgorithm.RS512, PROVIDER_USER_ID, null);

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void malformed_token은_UNAUTHORIZED로_변환한다() {
        assertError("not-a-jwt", AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    void sub가_없거나_blank이면_UNAUTHORIZED로_변환한다(final String subject) {
        final String idToken = token(signingKey, SignatureAlgorithm.RS256, subject, null);

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void nonce가_다르면_UNAUTHORIZED로_변환한다() {
        final String idToken = token(
                signingKey,
                SignatureAlgorithm.RS256,
                KakaoOAuthClientConfiguration.KAKAO_ISSUER,
                CLIENT_ID,
                NOW.plusSeconds(300),
                PROVIDER_USER_ID,
                null,
                "other-nonce");

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void nonce가_없으면_UNAUTHORIZED로_변환한다() {
        final String idToken = token(
                signingKey,
                SignatureAlgorithm.RS256,
                KakaoOAuthClientConfiguration.KAKAO_ISSUER,
                CLIENT_ID,
                NOW.plusSeconds(300),
                PROVIDER_USER_ID,
                null,
                null);

        assertError(idToken, AuthErrorCode.INVALID_ID_TOKEN, ErrorCode.Category.UNAUTHORIZED);
    }

    @Test
    void JWKS_조회_실패는_EXTERNAL_SERVICE_ERROR로_변환한다() {
        final JwtDecoder failedDecoder = mock(JwtDecoder.class);
        final RemoteKeySourceException remoteFailure =
                new RemoteKeySourceException("JWKS endpoint unavailable", new RuntimeException("network"));
        when(failedDecoder.decode("id-token")).thenThrow(new JwtException("decode failed", remoteFailure));
        final var failedClient = new KakaoOAuthProviderClientImpl(failedDecoder);

        assertThatThrownBy(() -> failedClient.verify(ProviderType.KAKAO, "id-token", EXPECTED_NONCE))
                .isInstanceOfSatisfying(BusinessException.class, (final var exception) -> {
                    assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
                    assertThat(exception.errorCode().category()).isEqualTo(ErrorCode.Category.EXTERNAL_SERVICE_ERROR);
                });
    }

    private JwtDecoder decoder(final RSAKey publicKey) {
        final JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(publicKey));
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(jwkSource)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(KakaoOAuthClientConfiguration.kakaoJwtValidator(CLIENT_ID));
        return decoder;
    }

    private String token(
            final RSAKey key, final SignatureAlgorithm algorithm, final String subject, final String email) {
        return token(
                key,
                algorithm,
                KakaoOAuthClientConfiguration.KAKAO_ISSUER,
                CLIENT_ID,
                NOW.plusSeconds(300),
                subject,
                email,
                EXPECTED_NONCE);
    }

    private String token(
            final RSAKey key,
            final SignatureAlgorithm algorithm,
            final String issuer,
            final String audience,
            final Instant expiresAt,
            final String subject,
            final String email,
            final String nonce) {
        final JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(expiresAt.minusSeconds(60))
                .expiresAt(expiresAt);
        if (subject != null) {
            claims.subject(subject);
        }
        if (email != null) {
            claims.claim("email", email);
        }
        if (nonce != null) {
            claims.claim("nonce", nonce);
        }
        final JwsHeader headers =
                JwsHeader.with(algorithm).keyId(key.getKeyID()).build();
        final JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(key));
        final NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSource);
        return encoder.encode(JwtEncoderParameters.from(headers, claims.build()))
                .getTokenValue();
    }

    private void assertError(final String idToken, final AuthErrorCode errorCode, final ErrorCode.Category category) {
        assertThatThrownBy(() -> client.verify(ProviderType.KAKAO, idToken, EXPECTED_NONCE))
                .isInstanceOfSatisfying(BusinessException.class, (final var exception) -> {
                    assertThat(exception.errorCode()).isEqualTo(errorCode);
                    assertThat(exception.errorCode().category()).isEqualTo(category);
                });
    }
}
