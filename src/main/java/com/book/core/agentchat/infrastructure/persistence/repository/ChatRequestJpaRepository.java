package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.domain.ChatRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRequestJpaRepository extends JpaRepository<ChatRequest, Long> {
    @Query("select r from ChatRequest r where r.sessionId = :sessionId and r.requestKey = :requestKey")
    Optional<ChatRequest> findByKey(Long sessionId, String requestKey);

    @Query("select r from ChatRequest r where r.id = :requestId")
    Optional<ChatRequest> findForChangeById(Long requestId);
}
