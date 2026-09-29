package com.book.core.search.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.logging.TraceIdFilter;
import com.book.core.search.application.port.BookSearchClient;
import com.book.core.search.application.port.BookSearchItem;
import com.book.core.search.application.port.BookSearchRequest;
import com.book.core.search.application.port.BookSearchResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@code POST /search} 연동.
 */
@Component
public class BookSearchClientImpl implements BookSearchClient {
    private static final String SEARCH_PATH = "/search";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String DEGRADED_HEADER = "X-Degraded";

    private final RestClient restClient;

    public BookSearchClientImpl(@Qualifier(AiRestClientConfig.DEFAULT_AI_REST_CLIENT) final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public BookSearchResult search(final BookSearchRequest request) {
        try {
            return restClient.post().uri(SEARCH_PATH).header(REQUEST_ID_HEADER, currentRequestId()).contentType(MediaType.APPLICATION_JSON)
                .body(AiSearchRequest.from(request)).exchange((httpRequest, httpResponse) -> {
                    final HttpStatusCode status = httpResponse.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        final AiSearchEnvelope envelope = httpResponse.bodyTo(AiSearchEnvelope.class);
                        if (envelope == null || envelope.data() == null) {
                            throw new CoreException(ErrorCode.AI_SEARCH_FAILURE);
                        }
                        return toResult(envelope.data(), httpResponse.getHeaders().getFirst(DEGRADED_HEADER));
                    }
                    if (status.value() == 400) {
                        throw new CoreException(ErrorCode.INVALID_REQUEST);
                    }
                    if (status.value() == 401) {
                        throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
                    }
                    if (status.value() == 410) {
                        throw new CoreException(ErrorCode.AI_SEARCH_CURSOR_EXPIRED);
                    }
                    throw new CoreException(ErrorCode.AI_SEARCH_FAILURE);
                });
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_SEARCH_TIMEOUT, exception);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_SEARCH_FAILURE, exception);
        }
    }

    private String currentRequestId() {
        final String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private BookSearchResult toResult(final AiSearchResponse response, final String degraded) {
        final List<BookSearchItem> items =
            Optional.ofNullable(response.results()).orElseGet(List::of).stream().map(item -> new BookSearchItem(item.bookId(), item.title(),
                item.author(), item.publisher(), item.price(), item.inStock(), item.coverUrl())).toList();
        final String fallbackMessage = response.fallback() == null ? null : response.fallback().message();
        return new BookSearchResult(items, response.nextCursor(), fallbackMessage, degraded);
    }
}
