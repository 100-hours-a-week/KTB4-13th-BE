package com.book.core.agentchat.application.port;

import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatRepositoryPort {
    ChatSession saveSession(ChatSession session);

    Optional<ChatSession> findSession(Long sessionId);

    List<ChatSession> findSessions(Long userId, Long beforeId, int limit);

    Optional<ChatSession> findSessionForChange(Long sessionId);

    ChatRequest saveRequest(ChatRequest request);

    Optional<ChatRequest> findRequestForChange(Long sessionId, String requestKey);

    ChatRequest getRequestForChange(Long requestId);

    void saveMessages(List<ChatMessage> messages);

    List<ChatMessage> recentMessages(Long sessionId, int limit);

    List<ChatMessage> messagesAfter(Long sessionId, long afterSequence, int limit);

    Optional<ChatMessage> latestCardResponse(Long sessionId);

    Optional<ChatMessage> responseFor(Long requestId);

    /** 기준 시각 전에 저장됐지만 아직 기억 추출에 반영하지 않은 메시지가 있는 세션을 마지막 메시지 시각 순으로 찾습니다. */
    List<MemoryExtractionTarget> findMemoryExtractionTargets(Instant before);
}
