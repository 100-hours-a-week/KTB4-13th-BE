package com.book.core.recommendation.infrastructure.client.ai;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiServerProperties.class)
class RestClientConfig {
    static final String REST_CLIENT = "recommendationAiRestClient";

    @Bean(REST_CLIENT)
    RestClient restClient(final AiServerProperties properties) {
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.serviceToken())
            .defaultStatusHandler(status -> status.equals(HttpStatus.UNAUTHORIZED), (request, response) -> {
                throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
            }).build();
    }
}
