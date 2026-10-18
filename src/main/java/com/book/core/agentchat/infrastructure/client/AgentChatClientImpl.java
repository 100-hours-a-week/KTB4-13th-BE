package com.book.core.agentchat.infrastructure.client;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.port.AgentChatClient;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentContextCard;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AgentChatClientImpl implements AgentChatClient {
    private final RestClient client;
    private final AgentChatProperties properties;
    private final AgentChatExternalMapper mapper;

    public AgentChatClientImpl(@Qualifier("agentShoppingRestClient") final RestClient client, final AgentChatProperties properties,
        final AgentChatExternalMapper mapper) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public AgentDecision chat(final ChatRecommendationCommand command, final Long sessionId, final Long requestId,
        final AgentChatContext context) {
        if (!enabled()) {
            throw new IllegalStateException("Agent shopping integration is disabled");
        }
        final List<AgentObservedProduct> known = context.products().stream()
            .filter((final AgentObservedProduct product) -> product.isbn() != null && product.isbn().matches("[0-9]{13}")).toList();
        if (context.products().stream()
            .anyMatch((final AgentObservedProduct product) -> product.quantity() > 0
                && (product.isbn() == null || !product.isbn().matches("[0-9]{13}")))
            || known.stream().map(AgentObservedProduct::isbn).distinct().count() != known.size() || known.stream().anyMatch(
                (final AgentObservedProduct product) -> product.price() != null && product.price().stripTrailingZeros().scale() > 0)) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
        final List<AgentAiCart> cart = known.stream().filter((final AgentObservedProduct product) -> product.quantity() > 0)
            .map((final AgentObservedProduct product) -> new AgentAiCart(product.isbn(), product.title(), product.price(),
                product.quantity(), product.active() && product.stockQuantity() >= product.quantity()))
            .toList();
        final Set<String> cardIsbns = context.cards().stream().map(AgentContextCard::isbn).collect(Collectors.toSet());
        final List<AgentAiOffer> offers = known.stream().filter((final AgentObservedProduct product) -> cardIsbns.contains(product.isbn()))
            .map((final AgentObservedProduct product) -> new AgentAiOffer(product.isbn(), product.title(), product.price(),
                product.active() && product.stockQuantity() > 0, product.stockQuantity()))
            .toList();
        final List<AgentAiContextCard> cards = context.cards().stream()
            .map((final AgentContextCard card) -> new AgentAiContextCard(card.isbn(), card.rank(), card.matchScore())).toList();
        final AgentAiRequest request =
            new AgentAiRequest(command.userId(), command.consented(), command.spec(), command.message(), command.recentTurns(),
                command.excludeBookIds(), "chat_" + requestId, "cv_" + sessionId, Map.of("logged_in", true), cards, null, cart, offers);
        try {
            final AgentAiEnvelope response =
                client.post().uri("/recommendations/chat").body(request).retrieve().body(AgentAiEnvelope.class);
            if (response == null) {
                throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
            }
            return mapper.map(response.data());
        } catch (final HttpStatusCodeException exception) {
            final int status = exception.getStatusCode().value();
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
}
