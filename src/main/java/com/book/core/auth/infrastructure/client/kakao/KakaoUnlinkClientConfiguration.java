package com.book.core.auth.infrastructure.client.kakao;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KakaoProperties.class)
class KakaoUnlinkClientConfiguration {
    static final String KAKAO_API_REST_CLIENT = "kakaoApiRestClient";
    static final String KAKAO_API_BASE_URL = "https://kapi.kakao.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean(KAKAO_API_REST_CLIENT)
    RestClient kakaoApiRestClient() {
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().baseUrl(KAKAO_API_BASE_URL).requestFactory(requestFactory).build();
    }
}
