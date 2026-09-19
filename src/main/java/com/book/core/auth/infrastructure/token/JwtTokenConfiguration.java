package com.book.core.auth.infrastructure.token;

import com.book.common.exception.BusinessException;
import com.book.core.auth.domain.exception.AuthErrorCode;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenProperties.class)
class JwtTokenConfiguration {
    static final String TOKEN_SECRET_KEY = "tokenSecretKey";
    static final String TOKEN_JWT_ENCODER = "tokenJwtEncoder";
    static final String SERVICE_JWT_DECODER = "serviceJwtDecoder";
    static final String TOKEN_CLOCK = "tokenClock";
    private static final int HS512_MINIMUM_KEY_BYTES = 64;
    private static final String HMAC_SHA_512 = "HmacSHA512";

    @Bean(TOKEN_SECRET_KEY)
    SecretKey tokenSecretKey(final TokenProperties properties) {
        return secretKey(properties.secret());
    }

    @Bean(TOKEN_JWT_ENCODER)
    JwtEncoder tokenJwtEncoder(@Qualifier(TOKEN_SECRET_KEY) final SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean(SERVICE_JWT_DECODER)
    JwtDecoder serviceJwtDecoder(
            @Qualifier(TOKEN_SECRET_KEY) final SecretKey secretKey, @Qualifier(TOKEN_CLOCK) final Clock clock) {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
        final JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setAllowEmptyExpiryClaim(false);
        timestampValidator.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator, new AccessTokenValidator()));
        return decoder;
    }

    @Bean(TOKEN_CLOCK)
    Clock tokenClock() {
        return Clock.systemUTC();
    }

    private SecretKey secretKey(final String encodedSecret) {
        final byte[] secretBytes;
        try {
            secretBytes = Base64.getDecoder().decode(encodedSecret);
        } catch (final IllegalArgumentException exception) {
            throw configurationFailure("auth.token.secret은 Base64 형식이어야 합니다.", exception);
        }
        if (secretBytes.length < HS512_MINIMUM_KEY_BYTES) {
            throw configurationFailure("auth.token.secret은 디코딩 후 64바이트 이상이어야 합니다.", null);
        }
        return new SecretKeySpec(secretBytes, HMAC_SHA_512);
    }

    private BusinessException configurationFailure(final String message, final Throwable cause) {
        return new BusinessException(AuthErrorCode.TOKEN_ISSUE_FAILURE, new IllegalStateException(message, cause));
    }
}
