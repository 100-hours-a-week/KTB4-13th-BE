package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FailChatRequestUseCase {
    private final ChatRepositoryPort chats;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final SendChatMessageCommand command, final long generation, final ErrorCode failure) {
        execute(command, generation, failure, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final SendChatMessageCommand command, final long generation, final ErrorCode failure,
        final Long retryAfterSeconds) {
        final ChatSession session = chats.findSession(command.sessionId()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        session.requireOwner(command.userId());
        final ChatRequest request =
            chats.findRequestForChange(session.id(), command.requestKey()).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
        request.requireSameInput(command.fingerprint());
        if (!request.running() || request.generation() != generation || !request.id().equals(session.activeRequestId())) {
            return;
        }
        chats.findSessionForChange(session.id());
        request.fail(generation, failure, retryAfterSeconds);
        session.release(request.id());
    }
}
