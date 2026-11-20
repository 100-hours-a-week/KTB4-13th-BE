package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration(proxyBeanMethods = false)
@EnableResilientMethods(proxyTargetClass = true)
@EnableConfigurationProperties(NewBookCollectorProperties.class)
class NewBookCollectorClientConfiguration {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    NationalLibraryClient nationalLibraryClient(final NewBookCollectorProperties properties) {
        return createClient(NationalLibraryClient.class, properties.nationalLibraryBaseUrl(), "nationalLibraryKey",
            properties.nationalLibraryKey(), READ_TIMEOUT);
    }

    @Bean
    JeongboNaruClient jeongboNaruClient(final NewBookCollectorProperties properties) {
        return createClient(JeongboNaruClient.class, properties.jeongboNaruBaseUrl(), "jeongboNaruKey", properties.jeongboNaruKey(),
            READ_TIMEOUT);
    }

    static <T> T createClient(
        final Class<T> type,
        final String baseUrl,
        final String keyVariable,
        final String key,
        final Duration readTimeout
    ) {
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(readTimeout);
        final RestClient restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
            .defaultUriVariables(Map.of(keyVariable, key))
            .defaultStatusHandler(HttpStatusCode::isError, (final var request, final var response) -> {
                if (response.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                    throw new NewBookCollectorException(NewBookCollectorFailureReason.RATE_LIMIT);
                }
                if (response.getStatusCode().is5xxServerError()) {
                    throw new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY);
                }
                throw new NewBookCollectorException(NewBookCollectorFailureReason.REJECTED);
            }).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(type);
    }
}
