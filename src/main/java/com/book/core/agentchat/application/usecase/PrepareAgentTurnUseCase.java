package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.AgentChatPayloadCodec;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentContextCard;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.agentchat.application.result.AgentPreparedTurn;
import com.book.core.agentchat.application.result.ChatCardSnapshot;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatSession;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.product.application.result.ProductOfferResult;
import com.book.core.product.application.usecase.GetProductOffersUseCase;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PrepareAgentTurnUseCase {
    private final ChatRepositoryPort chats;
    private final ChatResponseCodec responses;
    private final AgentChatPayloadCodec payloads;
    private final GetCartUseCase carts;
    private final GetProductOffersUseCase products;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentPreparedTurn execute(final SendChatMessageCommand command, final long generation) {
        final ChatRequest request = requireAttempt(command, generation);
        if (request.agentContext() == null) {
            request.saveAgentContext(payloads.encodeContext(observe(command)));
        }
        AgentDecision decision = null;
        if (request.agentDecision() != null) {
            decision = payloads.decodeDecision(request.agentDecision());
        }
        return new AgentPreparedTurn(payloads.decodeContext(request.agentContext()), decision);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentDecision saveDecision(final SendChatMessageCommand command, final long generation, final AgentDecision decision) {
        final ChatRequest request = requireAttempt(command, generation);
        if (request.agentDecision() == null) {
            request.saveAgentDecision(payloads.encodeDecision(decision));
        }
        return payloads.decodeDecision(request.agentDecision());
    }

    private ChatRequest requireAttempt(final SendChatMessageCommand command, final long generation) {
        final ChatSession session = chats.findSession(command.sessionId()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        session.requireOwner(command.userId());
        final ChatRequest request =
            chats.findRequestForChange(session.id(), command.requestKey()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        request.requireSameInput(command.fingerprint());
        session.requireActive(request.id());
        request.requireAttempt(generation, clock.instant());
        chats.findSessionForChange(session.id());
        return request;
    }

    private AgentChatContext observe(final SendChatMessageCommand command) {
        final List<GetCartItemResult> cart = carts.execute(command.userId()).items();
        final List<ChatCardSnapshot> cards = chats.latestCardResponse(command.sessionId())
            .map((final ChatMessage message) -> responses.decode(message.responsePayload()).cards()).orElse(List.of());
        final LinkedHashSet<Long> ids = new LinkedHashSet<>();
        cart.forEach((final GetCartItemResult item) -> ids.add(item.productId()));
        cards.stream().limit(10).filter((final ChatCardSnapshot card) -> card.productId() != null)
            .forEach((final ChatCardSnapshot card) -> ids.add(card.productId()));
        final Map<Long, Integer> quantities =
            cart.stream().collect(Collectors.toMap(GetCartItemResult::productId, GetCartItemResult::quantity));
        final List<ProductOfferResult> offers = products.execute(List.copyOf(ids));
        final Set<Long> existing = offers.stream().map(ProductOfferResult::productId).collect(Collectors.toSet());
        if (cart.stream().anyMatch((final GetCartItemResult item) -> !existing.contains(item.productId()))) {
            throw new CoreException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        final List<AgentObservedProduct> observed =
            offers.stream().map((final ProductOfferResult offer) -> new AgentObservedProduct(offer.productId(), offer.isbn(), offer.title(),
                offer.price(), quantities.getOrDefault(offer.productId(), 0), offer.stockQuantity(), offer.active())).toList();
        final Map<Long, ProductOfferResult> byId =
            offers.stream().collect(Collectors.toMap(ProductOfferResult::productId, Function.identity()));
        final List<AgentContextCard> contextCards = new ArrayList<>();
        for (int index = 0; index < Math.min(10, cards.size()); index++) {
            final ChatCardSnapshot card = cards.get(index);
            final ProductOfferResult offer = byId.get(card.productId());
            if (offer != null && offer.isbn() != null && offer.isbn().matches("[0-9]{13}")) {
                contextCards.add(new AgentContextCard(offer.isbn(), index + 1, card.matchScore()));
            }
        }
        return new AgentChatContext(observed, contextCards);
    }
}
