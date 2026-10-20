package com.book.core.memory.application.port;

public interface MemoryExtractionProgressPort {
    boolean markExtracted(final Long userId, final Long sessionId, final long sequence);
}
