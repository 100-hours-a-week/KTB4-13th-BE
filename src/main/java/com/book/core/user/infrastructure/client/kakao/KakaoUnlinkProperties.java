package com.book.core.user.infrastructure.client.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("auth.kakao")
record KakaoUnlinkProperties(String adminKey) {
}
