package com.book.core.recommendation.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.logging.TraceIdFilter;
import com.book.core.recommendation.application.port.RecommendationFeedClient;
import com.book.core.recommendation.application.port.RecommendationFeedItem;
import com.book.core.recommendation.application.port.RecommendationFeedRequest;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@code GET /recommendations/feed} 연동. 사용자가 홈 화면에서 직접 기다리는 동기 조회라 채팅(30초)이 아닌 공용
 * {@link AiRestClientConfig#DEFAULT_AI_REST_CLIENT}(5초) 타임아웃을 재사용한다. surface=home에서는 AI 계약상
 * sort/category/pub_year_from/pub_year_to/match_score_min을 절대 전달하지 않는다 — 하나라도 present하면 AI가 400을
 * 준다.
 */
@Component
public class RecommendationFeedClientImpl implements RecommendationFeedClient {
    private static final String FEED_PATH = "/recommendations/feed";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String DEGRADED_HEADER = "X-Degraded";
    private static final String SURFACE_HOME = "home";

    private final RestClient restClient;

    public RecommendationFeedClientImpl(@Qualifier(AiRestClientConfig.DEFAULT_AI_REST_CLIENT) final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RecommendationFeedResult getFeed(final RecommendationFeedRequest request) {
        try {
            return restClient.get().uri(uriBuilder -> {
                uriBuilder.path(FEED_PATH).queryParam("user_id", request.userId()).queryParam("surface", SURFACE_HOME).queryParam("size",
                    request.size());
                if (request.cursor() != null) {
                    uriBuilder.queryParam("cursor", request.cursor());
                }
                return uriBuilder.build();
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
        final String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private RecommendationFeedResult toResult(final AiFeedResponse response, final String degraded) {
        final List<RecommendationFeedItem> items =
            Optional.ofNullable(response.items()).orElseGet(List::of).stream().map(item -> new RecommendationFeedItem(item.bookId(),
                item.title(), item.author(), item.price(), item.coverUrl(), item.inStock(), item.matchScore())).toList();
        return new RecommendationFeedResult(items, response.nextCursor(), response.coldStart(), degraded);
    }
}
