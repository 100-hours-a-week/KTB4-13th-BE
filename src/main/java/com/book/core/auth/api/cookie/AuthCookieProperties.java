package com.book.core.auth.api.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth.cookie")
public record AuthCookieProperties(boolean secure) {}
