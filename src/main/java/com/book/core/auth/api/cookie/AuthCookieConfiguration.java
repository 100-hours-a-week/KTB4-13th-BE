package com.book.core.auth.api.cookie;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthCookieProperties.class)
class AuthCookieConfiguration {}
