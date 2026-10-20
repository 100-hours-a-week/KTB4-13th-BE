package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.memory.application.port.MemoryExtractionProgressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ChatMemoryExtractionProgressAdapter implements MemoryExtractionProgressPort {
    private final ChatSessionJpaRepository sessions;

    @Override
    public boolean markExtracted(final Long userId, final Long sessionId, final long sequence) {
        return sessions.markMemoryExtracted(userId, sessionId, sequence) == 1;
    }
}
