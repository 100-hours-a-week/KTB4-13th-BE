package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.port.ChatRepositoryPort;
import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import com.book.core.agentchat.domain.ChatMessage;
import com.book.core.agentchat.domain.ChatRequest;
import com.book.core.agentchat.domain.ChatSession;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatRepositoryAdapter implements ChatRepositoryPort {
    private final ChatSessionJpaRepository sessions;
    private final ChatRequestJpaRepository requests;
    private final ChatMessageJpaRepository messages;

    @Override
    public ChatSession saveSession(final ChatSession session) {
        return sessions.save(session);
    }

    @Override
    public Optional<ChatSession> findSession(final Long sessionId) {
        return sessions.findById(sessionId);
    }

    @Override
    public List<ChatSession> findSessions(final Long userId, final Long beforeId, final int limit) {
        return sessions.findSessions(userId, beforeId, PageRequest.ofSize(limit));
    }

    @Override
    public Optional<ChatSession> findSessionForChange(final Long sessionId) {
        return sessions.findForChangeById(sessionId);
    }

    @Override
    public ChatRequest saveRequest(final ChatRequest request) {
        return requests.save(request);
    }

    @Override
    public Optional<ChatRequest> findRequestForChange(final Long sessionId, final String requestKey) {
        return requests.findByKey(sessionId, requestKey);
    }

    @Override
    public ChatRequest getRequestForChange(final Long requestId) {
        return requests.findForChangeById(requestId).orElseThrow(() -> new CoreException(ErrorCode.CHAT_NOT_FOUND));
    }

    @Override
    public void saveMessages(final List<ChatMessage> values) {
        messages.saveAll(values);
    }

    @Override
    public List<ChatMessage> recentMessages(final Long sessionId, final int limit) {
        return messages.findBySessionIdOrderBySequenceDesc(sessionId, PageRequest.ofSize(limit)).reversed();
    }

    @Override
    public List<ChatMessage> messagesAfter(final Long sessionId, final long afterSequence, final int limit) {
        return messages.findBySessionIdAndSequenceGreaterThanOrderBySequenceAsc(sessionId, afterSequence, PageRequest.ofSize(limit));
    }

    @Override
    public Optional<ChatMessage> latestCardResponse(final Long sessionId) {
        return messages.latestCardResponse(sessionId);
    }

    @Override
    public Optional<ChatMessage> responseFor(final Long requestId) {
        return messages.findByRequestIdAndRole(requestId, "assistant");
    }

    @Override
    public List<MemoryExtractionTarget> findMemoryExtractionTargets(final Instant before) {
        return sessions.findMemoryExtractionTargets(LocalDateTime.ofInstant(before, ZoneId.systemDefault()));
    }
}
