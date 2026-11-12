package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.BookCatalogException;
import com.book.core.book.application.port.BookCatalogFailureReason;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

final class BookCatalogHttp {
    private BookCatalogHttp() {}

    static RestClient connect(final String baseUrl) {
        final var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    static String get(final RestClient client, final Function<UriBuilder, URI> uri) {
        try {
            return client.get().uri(uri).retrieve()
                .onStatus((final var status) -> status.isError(), (final var request, final var response) -> {
                    final int status = response.getStatusCode().value();
                    if (status == 429) {
                        throw new BookCatalogException(BookCatalogFailureReason.RATE_LIMIT);
                    }
                    if (status >= 500) {
                        throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
                    }
                    throw new BookCatalogException(BookCatalogFailureReason.REJECTED);
                }).body(String.class);
        } catch (final RestClientException exception) {
            // 통신 예외에 인증키가 포함된 요청 URL이 들어갈 수 있습니다.
            throw new BookCatalogException(BookCatalogFailureReason.TEMPORARY);
        }
    }
}
