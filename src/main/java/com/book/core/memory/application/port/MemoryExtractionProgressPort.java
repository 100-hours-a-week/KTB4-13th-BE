package com.book.core.memory.application.port;

public interface MemoryExtractionProgressPort {
    /** 대화의 기억 추출 완료 순번을 더 큰 값으로만 갱신하고, 이미 같거나 큰 순번을 반영했으면 false를 반환합니다. */
    boolean markExtracted(final Long userId, final Long sessionId, final long sequence);
}
