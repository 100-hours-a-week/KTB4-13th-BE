package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.agentchat.application.command.GetChatSessionsCommand;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.result.ChatSessionPageResult;
import com.book.core.agentchat.application.result.ChatSessionSummary;
import com.book.core.agentchat.domain.ChatSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetChatSessionsUseCase {
    private final ChatRepositoryPort chats;

    @Transactional(readOnly = true)
    public ChatSessionPageResult execute(final GetChatSessionsCommand command) {
        final List<ChatSession> found = chats.findSessions(command.userId(), command.beforeId(), command.limit() + 1);
        final List<ChatSessionSummary> sessions = found.stream().limit(command.limit()).map(ChatSessionSummary::from).toList();
        Long nextBeforeId = null;
        if (found.size() > command.limit()) {
            nextBeforeId = sessions.getLast().sessionId();
        }
        return new ChatSessionPageResult(sessions, nextBeforeId);
    }
}
