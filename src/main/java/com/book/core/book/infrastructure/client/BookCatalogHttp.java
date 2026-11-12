package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.BookCatalogException.Reason;
import com.book.core.book.application.port.BookCatalogException;
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
                        throw new BookCatalogException(Reason.RATE_LIMIT);
                    }
                    if (status >= 500) {
                        throw new BookCatalogException(Reason.TEMPORARY);
                    }
                    throw new BookCatalogException(Reason.REJECTED);
                }).body(String.class);
        } catch (final RestClientException exception) {
            // Transport exceptions may contain the credential-bearing URL.
            throw new BookCatalogException(Reason.TEMPORARY);
        }
    }
}
