package com.book.core.recommendation.infrastructure.client.ai;

import com.book.common.config.ai.AiRequestBudget;
import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.result.RecommendationHistory;
import com.book.core.onboarding.application.usecase.GetRecommendationHistoryUseCase;
import com.book.core.recommendation.application.command.RecommendationSpec;
import com.book.core.recommendation.application.command.RecommendationTurn;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import com.book.core.recommendation.application.result.RecommendationExclusion;
import com.book.core.recommendation.application.result.RecommendationExclusionSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final Map<String, Object> INITIAL_SPEC = RecommendationSpec.initial();

    private final RestClient restClient;
    private final GetAiBookIdentifiersUseCase identifiers;
    private final GetRecommendationHistoryUseCase history;
    private final ObjectMapper json;

    public AiRecommendationClientImpl(@Qualifier(AiRestClientConfig.RECOMMENDATION_AI_REST_CLIENT) final RestClient restClient,
        final GetAiBookIdentifiersUseCase identifiers, final GetRecommendationHistoryUseCase history, final ObjectMapper json) {
        this.restClient = restClient;
        this.identifiers = identifiers;
        this.history = history;
        this.json = json;
    }

    @Override
    public AiRecommendationChatResult chat(final AiRecommendationChatRequest request) {
        final RecommendationHistory recommendationHistory = history.execute(request.userId());
        final List<RecommendationExclusion> exclusions =
            RecommendationExclusion.newestFirst(identifiers.findIsbns(request.excludeBookIds().reversed()), recommendationHistory.books());
        final List<AiChatTurn> turns = request.recentTurns().stream().map(this::toAiChatTurn).toList();
        AiChatResponse response = attempt(chatBody(request, recommendationHistory.consented(), request.spec(), turns, exclusions), false);
        if (response == null) {
            response = attempt(chatBody(request, recommendationHistory.consented(), INITIAL_SPEC, turns, exclusions), true);
        }
        return toResult(response);
    }


    private AiChatResponse attempt(final byte[] aiRequest, final boolean isRetry) {
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
                    if (status.value() == 422) {
                        final AiChatEnvelope envelope = response.bodyTo(AiChatEnvelope.class);
                        if (envelope != null && "spec_schema_violation".equals(envelope.message())) {
                            if (!isRetry) {
                                return null;
                            }
                            throw new CoreException(ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION);
                        }
                        throw new CoreException(ErrorCode.AI_RECOMMENDATION_FAILURE);
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
        final String requestId = MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    private byte[] chatBody(final AiRecommendationChatRequest request, final boolean consented, final Map<String, Object> spec,
        final List<AiChatTurn> turns, final List<RecommendationExclusion> exclusions) {
        return AiRequestBudget.fit(exclusions, (final List<RecommendationExclusion> selected) -> {
            final AiChatRequest body = new AiChatRequest(request.userId(), consented, spec, request.message(), turns,
                RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.CONVERSATION),
                RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.PURCHASE),
                RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.LIBRARY));
            try {
                return json.writeValueAsBytes(body);
            } catch (final JsonProcessingException exception) {
                throw new CoreException(ErrorCode.AI_RECOMMENDATION_FAILURE, exception);
            }
        }, (final byte[] body) -> body.length, AiRequestBudget.CHAT_BODY_BYTES, ErrorCode.AI_RECOMMENDATION_FAILURE);
    }

    private AiChatTurn toAiChatTurn(final RecommendationTurn turn) {
        return new AiChatTurn(turn.role(), turn.text());
    }

    private AiRecommendationChatResult toResult(final AiChatResponse response) {
        final Map<String, Long> bookIds = identifiers.findBookIds(response.cards().stream().map(AiChatCard::isbn).toList());
        final List<AiRecommendationCard> cards =
            response.cards().stream().filter((final AiChatCard card) -> card.isbn() != null && bookIds.containsKey(card.isbn()))
                .map((final AiChatCard card) -> new AiRecommendationCard(bookIds.get(card.isbn()), card.matchScore(), card.reasonShort(),
                    card.reasonLong()))
                .toList();
        return new AiRecommendationChatResult(response.spec(), response.reply(), cards, response.followup(), response.buttons(),
            response.degraded());
    }

}
