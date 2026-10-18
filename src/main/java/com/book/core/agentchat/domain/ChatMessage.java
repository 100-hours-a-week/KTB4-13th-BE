package com.book.core.agentchat.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 완료된 발화 쌍만 공개 이력에 추가합니다. */
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ChatMessage extends BaseEntity {
    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;
    @Column(name = "request_id", nullable = false, updatable = false)
    private Long requestId;
    @Column(name = "message_sequence", nullable = false, updatable = false)
    private long sequence;
    @Column(nullable = false, updatable = false, length = 10)
    private String role;
    @Lob
    @Column(nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String text;
    @Lob
    @Column(name = "response_payload", updatable = false, columnDefinition = "LONGTEXT")
    private String responsePayload;

    public ChatMessage(final Long sessionId, final Long requestId, final long sequence, final String role, final String text,
        final String responsePayload) {
        if (sessionId == null || sessionId <= 0 || requestId == null || requestId <= 0 || sequence <= 0 || text == null
            || !("user".equals(role) || "assistant".equals(role)) || ("user".equals(role) && responsePayload != null)
            || ("assistant".equals(role) && (responsePayload == null || responsePayload.isBlank()))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.sequence = sequence;
        this.role = role;
        this.text = text;
        this.responsePayload = responsePayload;
    }
}
