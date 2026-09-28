package com.book.core.recommendation.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.common.logging.TraceIdFilter;
import com.book.core.recommendation.application.command.RecommendationTurn;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiRecommendationClientImpl implements AiRecommendationClient {
    private static final String CHAT_PATH = "/recommendations/chat";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final long DEFAULT_RETRY_AFTER_SECONDS = 1;
    private static final Map<String, Object> INITIAL_SPEC = initialSpec();

    private final RestClient restClient;

    public AiRecommendationClientImpl(@Qualifier(AiRestClientConfig.RECOMMENDATION_AI_REST_CLIENT) final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AiRecommendationChatResult chat(final AiRecommendationChatRequest request) {
        final AiChatRequest aiRequest = toAiChatRequest(request);
        AiChatResponse response = attempt(aiRequest, false);
        if (response == null) {
            response = attempt(withInitialSpec(aiRequest), true);
        }
        return toResult(response);
    }

    private AiChatResponse attempt(final AiChatRequest aiRequest, final boolean isRetry) {
        try {
            return restClient.post().uri(CHAT_PATH).header(REQUEST_ID_HEADER, currentRequestId()).contentType(MediaType.APPLICATION_JSON)
                .body(aiRequest).exchange((httpRequest, response) -> {
                    final HttpStatusCode status = response.getStatusCode();
                    if (status.is2xxSuccessful()) {
                        final AiChatEnvelope envelope = response.bodyTo(AiChatEnvelope.class);
                        if (envelope == null || envelope.data() == null) {
                            throw new CoreException(ErrorCode.AI_RECOMMENDATION_FAILURE);
                        }
                        return envelope.data();
                    }
                    if (status.value() == 401) {
                        throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
                    }
                    if (status.value() == 422 && !isRetry) {
                        return null;
                    }
                    if (status.value() == 429) {
                        throw new RetryAfterException(ErrorCode.AI_RECOMMENDATION_RATE_LIMITED, retryAfterSeconds(response.getHeaders()));
                    }
                    if (status.value() == 503) {
                        throw new RetryAfterException(ErrorCode.AI_RECOMMENDATION_UNAVAILABLE, retryAfterSeconds(response.getHeaders()));
                    }
                    if (status.value() == 504) {
                        throw new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
                    }
                    if (status.value() == 422) {
                        throw new CoreException(ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION);
                    }
                    throw new CoreException(ErrorCode.AI_RECOMMENDATION_FAILURE);
                });
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT, exception);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_RECOMMENDATION_FAILURE, exception);
        }
    }

    private long retryAfterSeconds(final HttpHeaders headers) {
        final String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException exception) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
    }

    private String currentRequestId() {
        final String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : UUID.randomUUID().toString();
    }

    private AiChatRequest toAiChatRequest(final AiRecommendationChatRequest request) {
        final List<AiChatTurn> turns = request.recentTurns().stream().map(this::toAiChatTurn).toList();
        return new AiChatRequest(request.userId(), request.consented(), request.spec(), request.message(), turns, request.excludeBookIds());
    }

    private AiChatTurn toAiChatTurn(final RecommendationTurn turn) {
        return new AiChatTurn(turn.role(), turn.text());
    }

    private AiChatRequest withInitialSpec(final AiChatRequest request) {
        return new AiChatRequest(request.userId(), request.consented(), INITIAL_SPEC, request.message(), request.recentTurns(),
            request.excludeBookIds());
    }

    private AiRecommendationChatResult toResult(final AiChatResponse response) {
        final List<AiRecommendationCard> cards =
            response.cards().stream().map(card -> new AiRecommendationCard(card.bookId(), card.reasonLong())).toList();
        return new AiRecommendationChatResult(response.spec(), response.reply(), cards, response.followup(), response.buttons(),
            response.degraded());
    }

    private static Map<String, Object> initialSpec() {
        final Map<String, Object> exact = new HashMap<>();
        exact.put("title", null);
        exact.put("author", null);
        exact.put("publisher", null);

        final Map<String, Object> spec = new HashMap<>();
        spec.put("intent", "semantic");
        spec.put("exact", Collections.unmodifiableMap(exact));
        spec.put("filters", Map.of());
        spec.put("semantic", null);
        spec.put("anchor_book", null);
        spec.put("exclude", List.of());
        return Collections.unmodifiableMap(spec);
    }
}
