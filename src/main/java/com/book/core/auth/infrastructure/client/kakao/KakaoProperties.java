package com.book.core.auth.infrastructure.client.kakao;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Accessors(fluent = true)
@ConfigurationProperties("auth.kakao")
public class KakaoProperties {
    private final String restApiKey;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoProperties(final String restApiKey, final String clientSecret, final String redirectUri) {
        if (!StringUtils.hasText(restApiKey)) {
            throw new IllegalStateException("auth.kakao.rest-api-key 설정이 필요합니다.");
        }
        if (!StringUtils.hasText(redirectUri)) {
            throw new IllegalStateException("auth.kakao.redirect-uri 설정이 필요합니다.");
        }
        this.restApiKey = restApiKey;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }
}
