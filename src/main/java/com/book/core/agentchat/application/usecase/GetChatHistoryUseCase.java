package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetChatHistoryUseCase {
    private final ChatRepositoryPort chats;
    private final ChatResponseCodec codec;

    @Transactional(readOnly = true)
    public List<ChatHistoryMessage> execute(final Long userId, final Long sessionId, final long afterSequence, final int limit) {
        if (afterSequence < 0 || limit < 1 || limit > 100) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        chats.findSession(sessionId).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND)).requireOwner(userId);
        return chats.messagesAfter(sessionId, afterSequence, limit).stream().map(this::toHistoryMessage).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ChatHistoryMessage>> executeForMemoryExtraction(final List<MemoryExtractionTarget> targets) {
        final Map<Long, MemoryExtractionTarget> targetsBySessionId =
            targets.stream().collect(Collectors.toMap(MemoryExtractionTarget::sessionId, Function.identity()));
        final Map<Long, List<ChatHistoryMessage>> conversations = new HashMap<>();
        for (final ChatSession session : chats.findSessionsByIds(targetsBySessionId.keySet())) {
            if (session.userId().equals(targetsBySessionId.get(session.id()).userId())) {
                conversations.put(session.id(), new ArrayList<>());
            }
        }
        if (conversations.isEmpty()) {
            return conversations;
        }
        final long maxSequence = targets.stream().mapToLong(MemoryExtractionTarget::sequence).max().orElseThrow();
        for (final ChatMessage message : chats.messagesUpTo(conversations.keySet(), maxSequence)) {
            if (message.sequence() <= targetsBySessionId.get(message.sessionId()).sequence()) {
                conversations.get(message.sessionId()).add(toHistoryMessage(message));
            }
        }
        return conversations;
    }

    private ChatHistoryMessage toHistoryMessage(final ChatMessage message) {
        ChatResponseSnapshot response = null;
        if (message.responsePayload() != null) {
            response = codec.decode(message.responsePayload());
        }
        return new ChatHistoryMessage(message.id(), message.sequence(), message.role(), message.text(), message.createdAt(), response);
    }
}
