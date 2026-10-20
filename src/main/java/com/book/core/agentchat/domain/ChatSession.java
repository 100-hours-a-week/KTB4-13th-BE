package com.book.core.agentchat.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 같은 대화방의 처리 순서와 완료된 메시지 순서를 소유합니다. */
@Entity
@Table(name = "chat_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ChatSession extends BaseEntity {
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(name = "active_request_id")
    private Long activeRequestId;
    @Column(name = "next_sequence", nullable = false)
    private long nextSequence;
    @Column(name = "memory_extracted_sequence", nullable = false, insertable = false, updatable = false)
    private long memoryExtractedSequence;

    public ChatSession(final Long userId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_USER_ID);
        }
        this.userId = userId;
        this.nextSequence = 1;
    }

    public void requireOwner(final Long userId) {
        if (!this.userId.equals(userId)) {
            throw new CoreException(ErrorCode.CHAT_NOT_FOUND);
        }
    }

    public void begin(final Long requestId) {
        if (activeRequestId != null) {
            throw new CoreException(ErrorCode.CHAT_IN_PROGRESS);
        }
        if (requestId == null || requestId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        activeRequestId = requestId;
    }

    public void release(final Long requestId) {
        requireActive(requestId);
        activeRequestId = null;
    }

    public long complete(final Long requestId) {
        requireActive(requestId);
        final long sequence = nextSequence;
        nextSequence += 2;
        activeRequestId = null;
        return sequence;
    }

    public void requireActive(final Long requestId) {
        if (activeRequestId == null || !activeRequestId.equals(requestId)) {
            throw new CoreException(ErrorCode.CHAT_ATTEMPT_EXPIRED);
        }
    }
}
