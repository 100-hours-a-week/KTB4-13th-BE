package com.book.core.auth.infrastructure.client.kakao;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KakaoProperties.class)
class KakaoOAuthClientConfiguration {
    static final String KAKAO_JWT_DECODER = "kakaoJwtDecoder";
    static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    private static final String KAKAO_JWK_SET_URI = "https://kauth.kakao.com/.well-known/jwks.json";

    @Bean(KAKAO_JWT_DECODER)
    JwtDecoder kakaoJwtDecoder(final KakaoProperties properties) {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(KAKAO_JWK_SET_URI)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(kakaoJwtValidator(properties.clientId()));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> kakaoJwtValidator(final String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(KAKAO_ISSUER), new JwtAudienceValidator(clientId));
    }
}
