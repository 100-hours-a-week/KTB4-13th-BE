package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.domain.ChatMessage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderBySequenceDesc(Long sessionId, Pageable page);

    List<ChatMessage> findBySessionIdAndSequenceGreaterThanOrderBySequenceAsc(Long sessionId, long sequence, Pageable page);

    List<ChatMessage> findBySessionIdInAndSequenceLessThanEqualOrderBySessionIdAscSequenceAsc(Collection<Long> sessionIds, long sequence);

    @Query(value = "select * from chat_messages where session_id = :sessionId "
        + "and role = 'assistant' and deleted_at is null and JSON_LENGTH(JSON_EXTRACT(response_payload, '$.cards')) > 0 "
        + "order by message_sequence desc limit 1", nativeQuery = true)
    Optional<ChatMessage> latestCardResponse(Long sessionId);

    Optional<ChatMessage> findByRequestIdAndRole(Long requestId, String role);
}
