package com.book.core.agentchat.application.result;

/** 기억 추출에 보낼 세션과 이번에 반영할 마지막 메시지 순번입니다. */
public record MemoryExtractionTarget(Long userId, Long sessionId, long sequence) {
}
