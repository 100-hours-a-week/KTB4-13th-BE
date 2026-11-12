package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.PrepareCartChangeCommand;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.AgentAction;
import com.book.core.agentchat.application.result.AgentButton;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentCheckout;
import com.book.core.agentchat.application.result.AgentChosenBook;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentExecutionSummary;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.domain.CartChangeLine;
import com.book.core.agentchat.domain.CartChangeOperation;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.product.application.result.RecommendationProductResult;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.book.core.recommendation.application.usecase.ChatRecommendationUseCase;
import com.book.core.recommendation.application.usecase.SaveRecommendationCardsUseCase;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CompleteChatRequestUseCase {
    private final ChatRepositoryPort chats;
    private final ChatResponseCodec codec;
    private final SaveRecommendationCardsUseCase saveCards;
    private final Clock clock;
    private final PrepareCartChangeUseCase prepareCart;
    private final ExecuteCartChangeUseCase executeCart;
    private final ChatRecommendationUseCase recommendations;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatResponseSnapshot execute(
        final SendChatMessageCommand command,
        final long generation,
        final ChatRecommendationAiOutcome outcome
    ) {
        final ChatSession session = chats.findSession(command.sessionId()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        session.requireOwner(command.userId());
        final ChatRequest request =
            chats.findRequestForChange(session.id(), command.requestKey()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        request.requireSameInput(command.fingerprint());
        if (request.completed()) {
            return codec
                .decode(chats.responseFor(request.id()).orElseThrow(() -> new CoreException(ErrorCode.DEFAULT_ERROR)).responsePayload());
        }
        chats.findSessionForChange(session.id());
        session.requireActive(request.id());
        request.requireAttempt(generation, clock.instant());
        final List<RecommendationCardResult> cards = saveCards.execute(command.userId(), outcome.cards());
        final ChatResponseSnapshot response = ChatResponseSnapshot.from(ChatRecommendationResult.from(outcome, cards));
        final String payload = codec.encode(response);
        request.complete(generation, clock.instant());
        final long sequence = session.complete(request.id());
        chats.saveMessages(List.of(new ChatMessage(session.id(), request.id(), sequence, "user", command.message(), null),
            new ChatMessage(session.id(), request.id(), sequence + 1, "assistant", response.conversationText(), payload)));
        return response;
    }

    /** 장바구니 변경·제안·최종 화면·발화 쌍을 하나의 완료 트랜잭션으로 확정합니다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatResponseSnapshot executeAgent(
        final SendChatMessageCommand command,
        final long generation,
        final AgentDecision decision,
        final AgentChatContext context,
        final Map<String, RecommendationProductResult> newProducts,
        final Map<String, Object> previousSpec
    ) {
        final ChatSession session = chats.findSession(command.sessionId()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        session.requireOwner(command.userId());
        final ChatRequest request =
            chats.findRequestForChange(session.id(), command.requestKey()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        request.requireSameInput(command.fingerprint());
        if (request.completed()) {
            return codec
                .decode(chats.responseFor(request.id()).orElseThrow(() -> new CoreException(ErrorCode.DEFAULT_ERROR)).responsePayload());
        }
        session.requireActive(request.id());
        request.requireAttempt(generation, clock.instant());
        chats.findSessionForChange(session.id());
        final ChatResponseSnapshot response;
        if ("recommend".equals(decision.route())) {
            final ChatRecommendationAiOutcome outcome = recommendations.resolve(decision.recommendation());
            final List<RecommendationCardResult> cards = saveCards.execute(command.userId(), outcome.cards());
            response = ChatResponseSnapshot.agentRecommendation(ChatRecommendationResult.from(outcome, cards), decision.buttons());
        } else {
            response = completeShop(command, request.id(), decision, context, newProducts, previousSpec);
        }
        final String payload = codec.encode(response);
        request.complete(generation, clock.instant());
        final long sequence = session.complete(request.id());
        chats.saveMessages(List.of(new ChatMessage(session.id(), request.id(), sequence, "user", command.message(), null),
            new ChatMessage(session.id(), request.id(), sequence + 1, "assistant", response.conversationText(), payload)));
        return response;
    }

    private ChatResponseSnapshot completeShop(
        final SendChatMessageCommand command,
        final Long requestId,
        final AgentDecision decision,
        final AgentChatContext context,
        final Map<String, RecommendationProductResult> newProducts,
        final Map<String, Object> spec
    ) {
        if (decision.selection() == null && decision.actions().isEmpty()) {
            return ChatResponseSnapshot.shop(spec, decision.reply(), decision.buttons(), null, null);
        }
        final List<CartChangeLine> lines = new ArrayList<>();
        try {
            if (decision.selection() != null) {
                for (final AgentChosenBook book : decision.selection().chosen()) {
                    lines.add(changeLine(context.requireProduct(book.isbn()), 1));
                }
            } else {
                for (final AgentAction action : decision.actions()) {
                    if ("remove".equals(action.op())) {
                        final AgentObservedProduct product = context.requireProduct(action.isbn());
                        if (product.quantity() == 0) {
                            return declined(spec, ErrorCode.CART_STATE_CHANGED);
                        }
                        lines.add(new CartChangeLine(CartChangeOperation.REMOVE, product.productId(), product.quantity(), null, null));
                    } else {
                        lines.add(context.findProduct(action.isbn())
                            .map((final AgentObservedProduct product) -> changeLine(product, action.quantity()))
                            .orElseGet(() -> addNewProduct(newProducts.get(action.isbn()), action.quantity())));
                    }
                }
            }
        } catch (final CoreException exception) {
            if (exception.errorCode() == ErrorCode.PRODUCT_NOT_FOUND) {
                return declined(spec, exception.errorCode());
            }
            throw exception;
        }
        final PrepareCartChangeCommand changes = new PrepareCartChangeCommand(command.userId(), "agent:" + requestId, lines);
        if (decision.selection() != null) {
            final Long proposalId = prepareCart.createProposal(changes);
            final List<AgentButton> buttons = decision.buttons().stream().map((final AgentButton button) -> {
                if ("confirm_selection".equals(button.action())) {
                    return new AgentButton(button.label(), button.action(), null, proposalId);
                }
                return button;
            }).toList();
            return ChatResponseSnapshot.shop(spec, decision.reply(), buttons, new AgentExecutionSummary(null, proposalId, "proposed", null),
                null);
        }
        final CartChangeResult result = executeCart.execute(command.userId(), prepareCart.prepareDirect(changes));
        if (result.rejection() != null) {
            return declined(spec, result.rejection());
        }
        final boolean addedBooks = lines.stream().anyMatch((final CartChangeLine line) -> line.operation() != CartChangeOperation.REMOVE);
        final List<AgentButton> buttons = addedBooks ? decision.buttons()
            : decision.buttons().stream().filter((final AgentButton button) -> !button.opensCheckout()).toList();
        return ChatResponseSnapshot.shop(spec, decision.reply(), buttons,
            new AgentExecutionSummary(result.executionId(), null, "applied", null), validCheckout(decision.checkout(), context, buttons));
    }

    private AgentCheckout validCheckout(final AgentCheckout checkout, final AgentChatContext context, final List<AgentButton> buttons) {
        if (checkout == null || buttons.stream().noneMatch(AgentButton::opensCheckout) || checkout.usePoint().signum() < 0
            || checkout.usePoint().compareTo(context.pointBalance()) > 0) {
            return null;
        }
        if (checkout.useOwnedCouponId() != null
            && context.ownedCoupons().stream().map(OwnedCouponResult::id).noneMatch(checkout.useOwnedCouponId()::equals)) {
            return null;
        }
        return checkout;
    }

    private CartChangeLine addNewProduct(final RecommendationProductResult product, final int quantity) {
        if (product == null) {
            throw new CoreException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return new CartChangeLine(CartChangeOperation.ADD, product.productId(), 0, quantity, product.price());
    }

    private CartChangeLine changeLine(final AgentObservedProduct product, final int quantity) {
        if (!product.active() || product.price() == null) {
            throw new CoreException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        CartChangeOperation operation = CartChangeOperation.ADD;
        if (product.quantity() > 0) {
            operation = CartChangeOperation.UPDATE;
        }
        return new CartChangeLine(operation, product.productId(), product.quantity(), quantity, product.price());
    }

    private ChatResponseSnapshot declined(final Map<String, Object> spec, final ErrorCode error) {
        return ChatResponseSnapshot.shop(spec, error.message(), List.of(), new AgentExecutionSummary(null, null, "rejected", error.name()),
            null);
    }

}
