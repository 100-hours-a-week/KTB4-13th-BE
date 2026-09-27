package com.book.common.config.ai;

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
public class AiRestClientConfig {
    public static final String RECOMMENDATION_AI_REST_CLIENT = "recommendationAiRestClient";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration RECOMMENDATION_READ_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public RestClient aiRestClient(final AiServerProperties properties) {
        return restClientBuilder(properties, READ_TIMEOUT).build();
    }

    @Bean(RECOMMENDATION_AI_REST_CLIENT)
    public RestClient recommendationAiRestClient(final AiServerProperties properties) {
        return restClientBuilder(properties, RECOMMENDATION_READ_TIMEOUT).build();
    }

    static RestClient.Builder restClientBuilder(final AiServerProperties properties) {
        return restClientBuilder(properties, READ_TIMEOUT);
    }

    private static RestClient.Builder restClientBuilder(final AiServerProperties properties, final Duration readTimeout) {
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.serviceToken())
            .defaultStatusHandler(status -> status.equals(HttpStatus.UNAUTHORIZED), (request, response) -> {
                throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
            });
    }
}
