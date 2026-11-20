package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    @Bean
    NationalLibraryClient nationalLibraryClient(final NewBookCollectorProperties properties) {
        return createClient(NationalLibraryClient.class, properties.nationalLibraryBaseUrl(), "nationalLibraryKey",
            properties.nationalLibraryKey());
    }

    @Bean
    JeongboNaruClient jeongboNaruClient(final NewBookCollectorProperties properties) {
        return createClient(JeongboNaruClient.class, properties.dataForLibraryBaseUrl(), "jeongboNaruKey", properties.dataForLibraryKey());
    }

    static <T> T createClient(final Class<T> type, final String baseUrl, final String keyVariable, final String key) {
        final var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        final RestClient restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory)
            .defaultUriVariables(Map.of(keyVariable, key))
            .defaultStatusHandler(HttpStatusCode::isError, (final var request, final var response) -> {
                final int status = response.getStatusCode().value();
                if (status == 429) {
                    throw new NewBookCollectorException(NewBookCollectorFailureReason.RATE_LIMIT);
                }
                if (status >= 500) {
                    throw new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY);
                }
                throw new NewBookCollectorException(NewBookCollectorFailureReason.REJECTED);
            }).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(type);
    }
}
