package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.ChatAttemptResult;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.RecommendationSpec;
import com.book.core.recommendation.application.command.RecommendationTurn;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class BeginChatRequestUseCase {
    private static final Duration DEFAULT_PROCESSING_LEASE = Duration.ofSeconds(120);

    private final ChatRepositoryPort chats;
    private final ChatResponseCodec codec;
    private final Clock clock;

    /** 기존 AI의 최대 2회 호출 시간(연결 3초 + 읽기 30초)을 넘기는 HTTP 처리 기한입니다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatAttemptResult execute(final SendChatMessageCommand command) {
        return execute(command, DEFAULT_PROCESSING_LEASE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatAttemptResult execute(final SendChatMessageCommand command, final Duration lease) {
        final ChatSession session = chats.findSession(command.sessionId()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        session.requireOwner(command.userId());
        final Optional<ChatRequest> previous = chats.findRequestForChange(session.id(), command.requestKey());
        if (previous.isPresent()) {
            final ChatRequest request = previous.get();
            request.requireSameInput(command.fingerprint());
            if (request.completed()) {
                final ChatMessage response = chats.responseFor(request.id()).orElseThrow(() -> new CoreException(ErrorCode.DEFAULT_ERROR));
                return new ChatAttemptResult(request.id(), request.generation(), null, codec.decode(response.responsePayload()), null,
                    null);
            }
            if (!request.running()) {
                return new ChatAttemptResult(request.id(), request.generation(), null, null, ErrorCode.valueOf(request.failureCode()),
                    request.retryAfterSeconds());
            }
            chats.findSessionForChange(session.id());
            session.requireActive(request.id());
            request.resume(clock.instant(), lease);
            return attempt(command, request);
        }
        chats.findSessionForChange(session.id());
        if (session.activeRequestId() != null) {
            final ChatRequest active = chats.getRequestForChange(session.activeRequestId());
            active.abandon(clock.instant());
            session.release(active.id());
        }
        final ChatRequest request =
            chats.saveRequest(new ChatRequest(session.id(), command.requestKey(), command.fingerprint(), clock.instant(), lease));
        session.begin(request.id());
        return attempt(command, request);
    }

    private ChatAttemptResult attempt(final SendChatMessageCommand command, final ChatRequest request) {
        final List<ChatMessage> messages = chats.recentMessages(command.sessionId(), 20);
        Map<String, Object> spec = RecommendationSpec.initial();
        for (final ChatMessage message : messages.reversed()) {
            if (message.responsePayload() != null) {
                spec = codec.decode(message.responsePayload()).spec();
                break;
            }
        }
        final List<RecommendationTurn> recent = messages.stream()
            .map((final ChatMessage message) -> new RecommendationTurn(message.role(), contextText(message.text()))).toList();
        final ChatRecommendationCommand aiCommand =
            new ChatRecommendationCommand(command.userId(), command.consented(), spec, command.message(), recent, command.excludeBookIds());
        return new ChatAttemptResult(request.id(), request.generation(), aiCommand, null, null, null);
    }

    private static String contextText(final String text) {
        int start = Math.max(0, text.length() - 200);
        if (start > 0 && Character.isLowSurrogate(text.charAt(start))) {
            start++;
        }
        return text.substring(start);
    }

}
