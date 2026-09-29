package com.book.core.recommendation.infrastructure.client.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiServerProperties.class)
class RestClientConfig {
    static final String REST_CLIENT = "recommendationAiRestClient";

    @Bean(REST_CLIENT)
    RestClient restClient(final RestClient.Builder builder, final AiServerProperties properties) {
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.serviceToken()).build();
    }
}
