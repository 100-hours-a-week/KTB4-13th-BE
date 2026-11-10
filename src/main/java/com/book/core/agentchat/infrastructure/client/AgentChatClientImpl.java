package com.book.core.agentchat.infrastructure.client;

import com.book.common.config.ai.AiChatEventStream;
import com.book.common.config.ai.AiChatHttpClient;
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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AgentChatClientImpl implements AgentChatClient {
    private final AiChatHttpClient http;
    private final AgentChatProperties properties;
    private final AgentChatExternalMapper mapper;
    private final GetAiBookIdentifiersUseCase identifiers;
    private final GetRecommendationHistoryUseCase history;
    private final ObjectMapper json;

    public AgentChatClientImpl(final AiChatHttpClient http, final AgentChatProperties properties, final AgentChatExternalMapper mapper,
        final GetAiBookIdentifiersUseCase identifiers, final GetRecommendationHistoryUseCase history, final ObjectMapper json) {
        this.http = http;
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
                .map((final AgentObservedProduct product) -> new AgentAiCart(product.isbn(), product.productId(), product.title(),
                    product.price(), product.quantity(), product.active() && product.stockQuantity() >= product.quantity()))
                .toList();
            final Set<String> cardIsbns = cards.stream().map(AgentAiContextCard::isbn).collect(Collectors.toSet());
            offers = known.stream().filter((final AgentObservedProduct product) -> cardIsbns.contains(product.isbn()))
                .map((final AgentObservedProduct product) -> new AgentAiOffer(product.isbn(), product.productId(), product.title(),
                    product.price(), product.active() && product.stockQuantity() > 0, product.stockQuantity()))
                .toList();
            idempotencyKey = "chat_" + requestId;
        }
        final RecommendationHistory recommendationHistory = history.execute(command.userId());
        final List<RecommendationExclusion> exclusions =
            RecommendationExclusion.newestFirst(identifiers.findIsbns(command.excludeBookIds().reversed()), recommendationHistory.books());
        final AgentAiRequest request =
            new AgentAiRequest(command.userId(), recommendationHistory.consented(), command.spec(), command.message(),
                command.recentTurns(), List.of(), List.of(), List.of(), idempotencyKey, "cv_" + sessionId, Map.of("logged_in", true), cards,
                null, cart, offers, context.ownedCoupons().stream().map(AgentAiOwnedCoupon::from).toList(), context.pointBalance());
        return attempt(request, exclusions, false, onDelta, AiChatHttpClient.deadline());
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
            request.offers(), request.ownedCoupons(), request.pointBalance());
        try {
            return json.writeValueAsBytes(body);
        } catch (final JacksonException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE, exception);
        }
    }

    private AgentDecision attempt(final AgentAiRequest request, final List<RecommendationExclusion> exclusions, final boolean retried,
        final Consumer<String> onDelta, final long deadline) {
        try {
            final byte[] body = AiRequestBudget.fit(exclusions, (final List<RecommendationExclusion> selected) -> encode(request, selected),
                (final byte[] payload) -> payload.length, AiRequestBudget.CHAT_BODY_BYTES, ErrorCode.AI_SERVICE_FAILURE);
            final AgentAiEnvelope response = readEnvelope(body, onDelta, deadline);
            if (response == null) {
                if (retried) {
                    throw new CoreException(ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION);
                }
                final AgentAiRequest reset = new AgentAiRequest(request.userId(), request.consented(), RecommendationSpec.initial(),
                    request.message(), request.recentTurns(), List.of(), List.of(), List.of(), request.idempotencyKey(),
                    request.conversationId(), request.userContext(), request.contextCards(), request.focusedIsbn(), request.cart(),
                    request.offers(), request.ownedCoupons(), request.pointBalance());
                return attempt(reset, exclusions, true, onDelta, deadline);
            }
            final AgentDecision decision = mapper.map(response.data(), request.idempotencyKey() == null);
            if ("shop".equals(decision.route())) {
                validateActionIsbns(request, decision);
            }
            return decision;
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
    }

    private AgentAiEnvelope readEnvelope(final byte[] payload, final Consumer<String> onDelta, final long deadline) {
        final RestClient.RequestBodySpec request =
            http.before(deadline).post().uri("/recommendations/chat").contentType(MediaType.APPLICATION_JSON);
        if (onDelta != null) {
            request.accept(MediaType.TEXT_EVENT_STREAM);
        }
        return request.body(payload).exchange((final var httpRequest, final var response) -> {
            try (final InputStream body = response.getBody()) {
                final HttpStatusCode status = response.getStatusCode();
                if (status.is2xxSuccessful()) {
                    if (onDelta != null && MediaType.TEXT_EVENT_STREAM.includes(response.getHeaders().getContentType())) {
                        return AiChatEventStream.read(body, json, AgentAiEnvelope.class, onDelta, ErrorCode.AI_SERVICE_FAILURE);
                    }
                    return AiChatEventStream.readJson(body, json, AgentAiEnvelope.class, ErrorCode.AI_SERVICE_FAILURE);
                }
                if (status.value() == 422) {
                    final AgentAiEnvelope error =
                        AiChatEventStream.readJson(body, json, AgentAiEnvelope.class, ErrorCode.AI_SERVICE_FAILURE);
                    if ("spec_schema_violation".equals(error.message())) {
                        return null;
                    }
                }
                if (status.value() == 401) {
                    throw new CoreException(ErrorCode.AI_SERVICE_UNAUTHORIZED);
                }
                if (status.value() == 429 || status.value() == 503) {
                    final ErrorCode error =
                        status.value() == 429 ? ErrorCode.AI_RECOMMENDATION_RATE_LIMITED : ErrorCode.AI_RECOMMENDATION_UNAVAILABLE;
                    final String retry = response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
                    if (retry != null && retry.matches("[0-9]{1,9}")) {
                        throw new RetryAfterException(error, Long.parseLong(retry));
                    }
                    throw new CoreException(error);
                }
                if (status.value() == 504) {
                    throw new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT);
                }
                throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
            }
        });
    }

    private static void validateActionIsbns(final AgentAiRequest request, final AgentDecision decision) {
        final Set<String> sent =
            Stream.concat(request.cart().stream().map(AgentAiCart::isbn), request.offers().stream().map(AgentAiOffer::isbn))
                .collect(Collectors.toSet());
        if (decision.actions().stream().filter((final AgentAction action) -> "remove".equals(action.op())).map(AgentAction::isbn)
            .anyMatch((final String isbn) -> !sent.contains(isbn))
            || (decision.selection() != null && decision.selection().chosen().stream().map(AgentChosenBook::isbn)
                .anyMatch((final String isbn) -> !sent.contains(isbn)))) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
    }

}
