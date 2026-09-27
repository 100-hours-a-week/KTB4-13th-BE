package com.book.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("app.cors")
public class CorsProperties {
    private final String allowedOrigin;

    public CorsProperties(final String allowedOrigin) {
        if (!StringUtils.hasText(allowedOrigin)) {
            throw new IllegalStateException("app.cors.allowed-origin 설정이 필요합니다.");
        }
        this.allowedOrigin = allowedOrigin;
    }

    public String allowedOrigin() {
        return allowedOrigin;
    }
}
