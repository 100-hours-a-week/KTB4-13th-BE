package com.book.core.recommendation.infrastructure.client.ai;

import com.book.common.config.ai.AiRequestBudget;
import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.usecase.GetRecommendationHistoryUseCase;
import com.book.core.recommendation.application.port.RecommendationFeedClient;
import com.book.core.recommendation.application.port.RecommendationFeedItem;
import com.book.core.recommendation.application.port.RecommendationFeedRequest;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import com.book.core.recommendation.application.result.RecommendationExclusion;
import com.book.core.recommendation.application.result.RecommendationExclusionSource;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@code GET /recommendations/feed} 연동.
 */
@Component
public class RecommendationFeedClientImpl implements RecommendationFeedClient {
    // Reserve room for the signed AI cursor on every page so it cannot change the fitted history.
    private static final int CURSOR_QUERY_BUDGET_BYTES = 256;
    private static final String FEED_PATH = "/recommendations/feed";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String DEGRADED_HEADER = "X-Degraded";

    private final RestClient restClient;
    private final GetAiBookIdentifiersUseCase identifiers;
    private final GetRecommendationHistoryUseCase history;
    private final int requestTargetMaxBytes;

    public RecommendationFeedClientImpl(@Qualifier(AiRestClientConfig.DEFAULT_AI_REST_CLIENT) final RestClient restClient,
        final GetAiBookIdentifiersUseCase identifiers, final GetRecommendationHistoryUseCase history,
        @Value("${ai.feed-request-target-max-bytes:4096}") final int requestTargetMaxBytes) {
        this.restClient = restClient;
        this.identifiers = identifiers;
        this.history = history;
        this.requestTargetMaxBytes = requestTargetMaxBytes;
    }

    @Override
    public RecommendationFeedResult getFeed(final RecommendationFeedRequest request) {
        final List<RecommendationExclusion> exclusions =
            RecommendationExclusion.newestFirst(List.of(), history.execute(request.userId()).books());
        try {
            return restClient.get().uri(uriBuilder -> {
                final Map<String, String> variables = new HashMap<>();
                uriBuilder.path(FEED_PATH).queryParam("user_id", request.userId())
                    .queryParam("surface", request.surface().name().toLowerCase(Locale.ROOT)).queryParam("size", request.size())
                    .queryParamIfPresent("sort", Optional.ofNullable(request.sort()).map(sort -> sort.name().toLowerCase(Locale.ROOT)))
                    .queryParamIfPresent("pub_year_from", Optional.ofNullable(request.pubYearFrom()))
                    .queryParamIfPresent("pub_year_to", Optional.ofNullable(request.pubYearTo()))
                    .queryParamIfPresent("match_score_min", Optional.ofNullable(request.matchScoreMin()));
                if (request.category() != null) {
                    uriBuilder.queryParam("category", "{category}");
                    variables.put("category", request.category());
                }
                final int bytesWithoutCursor = requestTargetBytes(uriBuilder.build(variables));
                if (request.cursor() != null) {
                    uriBuilder.queryParam("cursor", "{cursor}");
                    variables.put("cursor", request.cursor());
                }
                final URI base = uriBuilder.build(variables);
                final int reserve = Math.max(0, CURSOR_QUERY_BUDGET_BYTES - (requestTargetBytes(base) - bytesWithoutCursor));
                return AiRequestBudget.fit(exclusions, (final List<RecommendationExclusion> selected) -> withExclusions(base, selected),
                    (final URI uri) -> requestTargetBytes(uri) + reserve, requestTargetMaxBytes, ErrorCode.AI_FEED_INVALID_REQUEST);
            }).header(REQUEST_ID_HEADER, currentRequestId()).exchange((httpRequest, httpResponse) -> {
                final HttpStatusCode status = httpResponse.getStatusCode();
                if (status.is2xxSuccessful()) {
                    final AiFeedEnvelope envelope = httpResponse.bodyTo(AiFeedEnvelope.class);
                    if (envelope == null || envelope.data() == null) {
                        throw new CoreException(ErrorCode.AI_FEED_FAILURE);
                    }
                    final String degraded = httpResponse.getHeaders().getFirst(DEGRADED_HEADER);
                    return toResult(envelope.data(), degraded);
                }
                if (status.value() == 401) {
                    throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
                }
                if (status.value() == 400) {
                    throw new CoreException(ErrorCode.AI_FEED_INVALID_REQUEST);
                }
                if (status.value() == 410) {
                    throw new CoreException(ErrorCode.AI_FEED_CURSOR_EXPIRED);
                }
                throw new CoreException(ErrorCode.AI_FEED_FAILURE);
            });
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_FEED_TIMEOUT, exception);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_FEED_FAILURE, exception);
        }
    }

    private String currentRequestId() {
        final String requestId = MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    private static URI withExclusions(final URI base, final List<RecommendationExclusion> selected) {
        return UriComponentsBuilder.fromUri(base)
            .queryParam("purchased_isbns",
                String.join(",", RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.PURCHASE)))
            .queryParam("library_isbns",
                String.join(",", RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.LIBRARY)))
            .build(true).toUri();
    }

    private static int requestTargetBytes(final URI uri) {
        // The raw path and query are percent-encoded ASCII, so each character occupies one byte.
        return uri.getRawPath().length() + 1 + uri.getRawQuery().length();
    }

    private RecommendationFeedResult toResult(final AiFeedResponse response, final String degraded) {
        final List<AiFeedItem> results = Optional.ofNullable(response.items()).orElseGet(List::of);
        final Map<String, Long> bookIds = identifiers.findBookIds(results.stream().map(AiFeedItem::isbn).toList());
        final List<RecommendationFeedItem> items =
            results.stream().filter((final AiFeedItem item) -> item.isbn() != null && bookIds.containsKey(item.isbn()))
                .map((final AiFeedItem item) -> new RecommendationFeedItem(bookIds.get(item.isbn()), item.title(), item.author(),
                    item.price(), item.coverUrl(), item.matchScore()))
                .toList();
        return new RecommendationFeedResult(items, response.nextCursor(), response.coldStart(), degraded);
    }
}
