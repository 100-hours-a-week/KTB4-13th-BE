package com.book.core.auth.infrastructure.client.kakao;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KakaoProperties.class)
class KakaoTokenClientConfiguration {
    static final String KAKAO_TOKEN_REST_CLIENT = "kakaoTokenRestClient";
    static final String KAKAO_ISSUER = "https://kauth.kakao.com";

    @Bean(KAKAO_TOKEN_REST_CLIENT)
    RestClient kakaoTokenRestClient() {
        return RestClient.builder().baseUrl(KAKAO_ISSUER).build();
    }
}
