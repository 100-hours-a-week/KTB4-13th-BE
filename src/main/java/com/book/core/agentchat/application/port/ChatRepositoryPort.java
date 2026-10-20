package com.book.core.agentchat.application.port;

import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatRepositoryPort {
    ChatSession saveSession(ChatSession session);

    Optional<ChatSession> findSession(Long sessionId);

    List<ChatSession> findSessions(Long userId, Long beforeId, int limit);

    List<ChatSession> findSessionsByIds(Collection<Long> sessionIds);

    Optional<ChatSession> findSessionForChange(Long sessionId);

    ChatRequest saveRequest(ChatRequest request);

    Optional<ChatRequest> findRequestForChange(Long sessionId, String requestKey);

    ChatRequest getRequestForChange(Long requestId);

    void saveMessages(List<ChatMessage> messages);

    List<ChatMessage> recentMessages(Long sessionId, int limit);

    List<ChatMessage> messagesAfter(Long sessionId, long afterSequence, int limit);

    List<ChatMessage> recentMessagesUpTo(Map<Long, Long> lastSequenceBySessionId, int limit);

    Optional<ChatMessage> latestCardResponse(Long sessionId);

    Optional<ChatMessage> responseFor(Long requestId);

    List<MemoryExtractionTarget> findMemoryExtractionTargets(Instant before);
}
