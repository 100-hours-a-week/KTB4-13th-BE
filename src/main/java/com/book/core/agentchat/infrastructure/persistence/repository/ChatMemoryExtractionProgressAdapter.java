package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.memory.application.port.MemoryExtractionProgressPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** 대화 세션이 소유한 기억 추출 완료 순번을 기억 기능의 저장 트랜잭션 안에서 갱신합니다. */
@Repository
@RequiredArgsConstructor
class ChatMemoryExtractionProgressAdapter implements MemoryExtractionProgressPort {
    private final ChatSessionJpaRepository sessions;

    @Override
    public boolean markExtracted(final Long userId, final Long sessionId, final long sequence) {
        return sessions.markMemoryExtracted(userId, sessionId, sequence) == 1;
    }
}
