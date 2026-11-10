package com.book.core.agentchat.infrastructure.client;

import com.book.common.config.ai.AiChatEventStream;
import com.book.common.config.ai.AiRequestBudget;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.port.AgentChatClient;
import com.book.core.agentchat.application.result.AgentAction;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentChosenBook;
import com.book.core.agentchat.application.result.AgentContextCard;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.result.RecommendationHistory;
import com.book.core.onboarding.application.usecase.GetRecommendationHistoryUseCase;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.RecommendationSpec;
import com.book.core.recommendation.application.result.RecommendationExclusion;
import com.book.core.recommendation.application.result.RecommendationExclusionSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AgentChatClientImpl implements AgentChatClient {
    private final RestClient client;
    private final AgentChatProperties properties;
    private final AgentChatExternalMapper mapper;
    private final GetAiBookIdentifiersUseCase identifiers;
    private final GetRecommendationHistoryUseCase history;
    private final ObjectMapper json;

    public AgentChatClientImpl(@Qualifier("agentShoppingRestClient") final RestClient client, final AgentChatProperties properties,
        final AgentChatExternalMapper mapper, final GetAiBookIdentifiersUseCase identifiers, final GetRecommendationHistoryUseCase history,
        final ObjectMapper json) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
        this.identifiers = identifiers;
        this.history = history;
        this.json = json;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public AgentDecision chat(final ChatRecommendationCommand command, final Long sessionId, final Long requestId,
        final AgentChatContext context) {
        return chat(command, sessionId, requestId, context, null);
    }

    @Override
    public AgentDecision chat(final ChatRecommendationCommand command, final Long sessionId, final Long requestId,
        final AgentChatContext context, final Consumer<String> onDelta) {
        if (!enabled()) {
            throw new IllegalStateException("Agent shopping integration is disabled");
        }
        final Set<String> validIsbns =
            identifiers.findUnambiguousIsbns(context.products().stream().map(AgentObservedProduct::isbn).toList());
        final List<AgentObservedProduct> known = context.products().stream()
            .filter((final AgentObservedProduct product) -> product.isbn() != null && validIsbns.contains(product.isbn())).toList();
        final boolean recommendationOnly =
            context.products().stream().anyMatch((final AgentObservedProduct product) -> product.quantity() > 0
                && (product.isbn() == null || !validIsbns.contains(product.isbn())));
        final List<AgentAiContextCard> cards =
            context.cards().stream().filter((final AgentContextCard card) -> card.isbn() != null && validIsbns.contains(card.isbn()))
                .map((final AgentContextCard card) -> new AgentAiContextCard(card.isbn(), card.rank(), card.matchScore())).toList();
        List<AgentAiCart> cart = List.of();
        List<AgentAiOffer> offers = List.of();
        String idempotencyKey = null;
        if (!recommendationOnly) {
            validateShoppingProducts(known);
            cart = known.stream().filter((final AgentObservedProduct product) -> product.quantity() > 0)
                .map((final AgentObservedProduct product) -> new AgentAiCart(product.isbn(), product.title(), product.price(),
                    product.quantity(), product.active() && product.stockQuantity() >= product.quantity()))
                .toList();
            final Set<String> cardIsbns = cards.stream().map(AgentAiContextCard::isbn).collect(Collectors.toSet());
            offers = known.stream().filter((final AgentObservedProduct product) -> cardIsbns.contains(product.isbn()))
                .map((final AgentObservedProduct product) -> new AgentAiOffer(product.isbn(), product.title(), product.price(),
                    product.active() && product.stockQuantity() > 0, product.stockQuantity()))
                .toList();
            idempotencyKey = "chat_" + requestId;
        }
        final RecommendationHistory recommendationHistory = history.execute(command.userId());
        final List<RecommendationExclusion> exclusions =
            RecommendationExclusion.newestFirst(identifiers.findIsbns(command.excludeBookIds().reversed()), recommendationHistory.books());
        final AgentAiRequest request = new AgentAiRequest(command.userId(), recommendationHistory.consented(), command.spec(),
            command.message(), command.recentTurns(), List.of(), List.of(), List.of(), idempotencyKey, "cv_" + sessionId,
            Map.of("logged_in", true), cards, null, cart, offers);
        return attempt(request, exclusions, false, onDelta);
    }

    private static void validateShoppingProducts(final List<AgentObservedProduct> products) {
        if (products.stream().map(AgentObservedProduct::isbn).distinct().count() != products.size() || products.stream().anyMatch(
            (final AgentObservedProduct product) -> product.price() != null && product.price().stripTrailingZeros().scale() > 0)) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
    }


    private byte[] encode(final AgentAiRequest request, final List<RecommendationExclusion> selected) {
        final AgentAiRequest body = new AgentAiRequest(request.userId(), request.consented(), request.spec(), request.message(),
            request.recentTurns(), RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.CONVERSATION),
            RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.PURCHASE),
            RecommendationExclusion.isbnsFrom(selected, RecommendationExclusionSource.LIBRARY), request.idempotencyKey(),
            request.conversationId(), request.userContext(), request.contextCards(), request.focusedIsbn(), request.cart(),
            request.offers());
        try {
            return json.writeValueAsBytes(body);
        } catch (final JsonProcessingException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

    private AgentDecision attempt(final AgentAiRequest request, final List<RecommendationExclusion> exclusions, final boolean retried,
        final Consumer<String> onDelta) {
        try {
            final byte[] body = AiRequestBudget.fit(exclusions, (final List<RecommendationExclusion> selected) -> encode(request, selected),
                (final byte[] payload) -> payload.length, AiRequestBudget.CHAT_BODY_BYTES, ErrorCode.AI_SERVICE_FAILURE);
            final AgentAiEnvelope response = readEnvelope(body, onDelta);
            if (response == null) {
                throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
            }
            final AgentDecision decision = mapper.map(response.data(), request.idempotencyKey() == null);
            if ("shop".equals(decision.route())) {
                validateActionIsbns(request, decision);
            }
            return decision;
        } catch (final HttpStatusCodeException exception) {
            final int status = exception.getStatusCode().value();
            // HTTP 상태 오류는 SSE 본문을 읽기 전에 처리하므로 이 분기에는 전달된 delta가 없습니다.
            if (status == 422 && specViolation(exception)) {
                if (retried) {
                    throw new CoreException(ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION);
                }
                final AgentAiRequest reset =
                    new AgentAiRequest(request.userId(), request.consented(), RecommendationSpec.initial(), request.message(),
                        request.recentTurns(), List.of(), List.of(), List.of(), request.idempotencyKey(), request.conversationId(),
                        request.userContext(), request.contextCards(), request.focusedIsbn(), request.cart(), request.offers());
                return attempt(reset, exclusions, true, onDelta);
            }
            if (status == 401) {
                throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
            }
            if (status == 429 || status == 503) {
                ErrorCode error = ErrorCode.AI_RECOMMENDATION_UNAVAILABLE;
                if (status == 429) {
                    error = ErrorCode.AI_RECOMMENDATION_RATE_LIMITED;
                }
                if (exception.getResponseHeaders() != null) {
                    final String retry = exception.getResponseHeaders().getFirst("Retry-After");
                    if (retry != null && retry.matches("[0-9]{1,9}")) {
                        throw new RetryAfterException(error, Long.parseLong(retry));
                    }
                }
                throw new CoreException(error);
            }
            if (status == 504) {
                throw new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
            }
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
    }

    private AgentAiEnvelope readEnvelope(final byte[] body, final Consumer<String> onDelta) {
        final RestClient.RequestBodySpec request = client.post().uri("/recommendations/chat").contentType(MediaType.APPLICATION_JSON);
        if (onDelta == null) {
            return request.body(body).retrieve().body(AgentAiEnvelope.class);
        }
        return request.accept(MediaType.TEXT_EVENT_STREAM).body(body).exchange((final var httpRequest, final var response) -> {
            if (response.getStatusCode().isError()) {
                new DefaultResponseErrorHandler().handleError(httpRequest.getURI(), httpRequest.getMethod(), response);
            }
            if (MediaType.TEXT_EVENT_STREAM.includes(response.getHeaders().getContentType())) {
                return AiChatEventStream.read(response, json, AgentAiEnvelope.class, onDelta, ErrorCode.AI_SERVICE_FAILURE);
            }
            return response.bodyTo(AgentAiEnvelope.class);
        });
    }

    private static void validateActionIsbns(final AgentAiRequest request, final AgentDecision decision) {
        final Set<String> sent =
            Stream.concat(request.cart().stream().map(AgentAiCart::isbn), request.offers().stream().map(AgentAiOffer::isbn))
                .collect(Collectors.toSet());
        if (decision.actions().stream().map(AgentAction::isbn).anyMatch((final String isbn) -> !sent.contains(isbn))
            || (decision.selection() != null && decision.selection().chosen().stream().map(AgentChosenBook::isbn)
                .anyMatch((final String isbn) -> !sent.contains(isbn)))) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
    }

    private boolean specViolation(final HttpStatusCodeException failure) {
        try {
            final AgentAiEnvelope error = json.readValue(failure.getResponseBodyAsString(), AgentAiEnvelope.class);
            return error != null && "spec_schema_violation".equals(error.message());
        } catch (final JsonProcessingException exception) {
            return false;
        }
    }

}
