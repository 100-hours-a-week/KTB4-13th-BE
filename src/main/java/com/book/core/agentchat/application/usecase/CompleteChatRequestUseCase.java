package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import com.book.core.recommendation.application.result.ChatRecommendationAiOutcome;
import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.book.core.recommendation.application.usecase.SaveRecommendationCardsUseCase;
import java.time.Clock;
import java.util.List;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatResponseSnapshot execute(final SendChatMessageCommand command, final long generation,
        final ChatRecommendationAiOutcome outcome) {
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
}
