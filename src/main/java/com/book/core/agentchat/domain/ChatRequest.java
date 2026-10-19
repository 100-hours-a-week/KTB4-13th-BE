package com.book.core.agentchat.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 원문을 보관하지 않는 중복 방지와 처리 시도 기록입니다. */
@Entity
@Table(name = "chat_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ChatRequest extends BaseEntity {
    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;
    @Column(name = "request_key", nullable = false, updatable = false, length = 100)
    private String requestKey;
    @Column(nullable = false, updatable = false, length = 64)
    private String fingerprint;
    @Column(nullable = false, length = 12)
    private String state;
    @Column(nullable = false)
    private long generation;
    @Column(name = "lease_until", nullable = false)
    private Instant leaseUntil;
    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Lob
    @Column(name = "agent_context", columnDefinition = "LONGTEXT")
    private String agentContext;
    @Lob
    @Column(name = "agent_decision", columnDefinition = "LONGTEXT")
    private String agentDecision;

    @Column(name = "retry_after_seconds")
    private Long retryAfterSeconds;

    public ChatRequest(final Long sessionId, final String requestKey, final String fingerprint, final Instant now, final Duration lease) {
        if (sessionId == null || sessionId <= 0 || requestKey == null || requestKey.isBlank() || requestKey.length() > 100
            || fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.sessionId = sessionId;
        this.requestKey = requestKey;
        this.fingerprint = fingerprint;
        this.state = "RUNNING";
        this.generation = 1;
        this.leaseUntil = deadline(now, lease);
    }

    public void requireSameInput(final String fingerprint) {
        if (!this.fingerprint.equals(fingerprint)) {
            throw new CoreException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }

    public boolean running() {
        return state.equals("RUNNING");
    }

    public boolean completed() {
        return state.equals("COMPLETED");
    }

    public boolean expired(final Instant now) {
        return running() && !now.isBefore(leaseUntil);
    }

    public void resume(final Instant now, final Duration lease) {
        if (!expired(now)) {
            throw new CoreException(ErrorCode.CHAT_IN_PROGRESS);
        }
        leaseUntil = deadline(now, lease);
        generation++;
    }

    public void requireAttempt(final long generation, final Instant now) {
        if (!running() || this.generation != generation || expired(now)) {
            throw new CoreException(ErrorCode.CHAT_ATTEMPT_EXPIRED);
        }
    }

    public void complete(final long generation, final Instant now) {
        requireAttempt(generation, now);
        state = "COMPLETED";
    }

    public void fail(final long generation, final ErrorCode error) {
        fail(generation, error, null);
    }

    public void fail(final long generation, final ErrorCode error, final Long retryAfterSeconds) {
        if (!running() || this.generation != generation) {
            throw new CoreException(ErrorCode.CHAT_ATTEMPT_EXPIRED);
        }
        if (error == null || (retryAfterSeconds != null && retryAfterSeconds < 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        failureCode = error.name();
        this.retryAfterSeconds = retryAfterSeconds;
        state = "FAILED";
    }

    public void abandon(final Instant now) {
        if (!expired(now)) {
            throw new CoreException(ErrorCode.CHAT_IN_PROGRESS);
        }
        failureCode = ErrorCode.CHAT_ATTEMPT_EXPIRED.name();
        state = "FAILED";
    }

    public void saveAgentContext(final String payload) {
        if (agentContext != null || payload == null || payload.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        agentContext = payload;
    }

    public void saveAgentDecision(final String payload) {
        if (agentContext == null || agentDecision != null || payload == null || payload.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        agentDecision = payload;
    }

    private static Instant deadline(final Instant now, final Duration lease) {
        if (now == null || lease == null || lease.isNegative() || lease.isZero()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return now.plus(lease);
    }
}
