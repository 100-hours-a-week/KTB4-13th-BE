package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderBySequenceDesc(Long sessionId, Pageable page);

    List<ChatMessage> findBySessionIdAndSequenceGreaterThanOrderBySequenceAsc(Long sessionId, long sequence, Pageable page);

    Optional<ChatMessage> findByRequestIdAndRole(Long requestId, String role);
}
