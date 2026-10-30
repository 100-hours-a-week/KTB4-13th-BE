package com.book.core.payment.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.HexFormat;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "pg_operation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class PgOperation extends BaseEntity {
    @Column(nullable = false, length = 255)
    private String orderKey;
    @Column(nullable = false, length = 64, unique = true)
    private String requestHash;
    @Column(nullable = false, length = 64)
    private String inputHash;
    @Column(nullable = false, length = 36, unique = true)
    private String pgIdempotencyKey;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PgOperationType type;
    @Column(nullable = false)
    private Instant requestedAt;
    @Column(nullable = false)
    private boolean completed;
    private Long resultId;
    @Column(nullable = false)
    private boolean rejected;
    @Column(length = 200)
    private String externalPaymentKey;
    @Column(precision = 19, scale = 2)
    private BigDecimal approvalAmount;
    @Column(length = 64)
    private String pgOrderId;
    private Long attemptId;
    private Instant uncertainAt;
    @Column(nullable = false)
    private boolean reviewRequired;
    @Column(nullable = false)
    private boolean blocksOrder = true;
    @Column(nullable = false)
    private boolean compensationRequested;
    @Column(length = 36)
    private String compensationIdempotencyKey;
    private Instant compensationRequestedAt;

    public PgOperation(final String orderKey, final String requestHash, final String inputHash, final PgOperationType type) {
        this.orderKey = orderKey;
        this.requestHash = requestHash;
        this.inputHash = inputHash;
        this.type = type;
        this.pgIdempotencyKey = UUID.randomUUID().toString();
        this.requestedAt = Instant.now();
        this.completed = false;
    }

    public void verifyInput(final String expectedOrderKey, final String expectedInputHash, final PgOperationType expectedType) {
        if (!orderKey.equals(expectedOrderKey) || !inputHash.equals(expectedInputHash) || type != expectedType) {
            throw new CoreException(ErrorCode.PG_REQUEST_CONFLICT);
        }
    }

    public void reject() {
        this.rejected = true;
    }

    public void recordApprovalDetails(final String paymentKey, final BigDecimal amount, final String pgOrderId, final Long attemptId) {
        if (type != PgOperationType.APPROVE || amount == null || amount.signum() < 0 || pgOrderId == null || pgOrderId.isBlank()
            || (amount.signum() > 0 && (paymentKey == null || paymentKey.isBlank()))) {
            throw new IllegalArgumentException("결제 확인에 필요한 정보가 올바르지 않습니다.");
        }
        this.externalPaymentKey = paymentKey;
        this.approvalAmount = amount;
        this.pgOrderId = pgOrderId;
        this.attemptId = attemptId;
    }

    public void recordUnidentifiedApprovalDetails(final BigDecimal amount, final String pgOrderId, final Long attemptId) {
        if (type != PgOperationType.APPROVE || amount == null || amount.signum() < 0 || pgOrderId == null || pgOrderId.isBlank()
            || attemptId == null) {
            throw new IllegalArgumentException("결제 확인에 필요한 시도 정보가 올바르지 않습니다.");
        }
        this.approvalAmount = amount;
        this.pgOrderId = pgOrderId;
        this.attemptId = attemptId;
    }

    /** Links a later callback to the same unresolved attempt without clearing operator review. */
    public boolean linkUnidentifiedApproval(final String orderKey, final Long attemptId, final String pgOrderId, final BigDecimal amount,
        final String paymentKey, final String requestHash, final String inputHash) {
        if (type != PgOperationType.APPROVE || completed || rejected || !reviewRequired || !blocksOrder || externalPaymentKey != null
            || !this.orderKey.equals(orderKey) || this.attemptId == null || !this.attemptId.equals(attemptId) || this.pgOrderId == null
            || !this.pgOrderId.equals(pgOrderId) || approvalAmount == null || amount == null || approvalAmount.compareTo(amount) != 0
            || paymentKey == null || paymentKey.isBlank() || requestHash == null || inputHash == null) {
            return false;
        }
        this.externalPaymentKey = paymentKey;
        this.requestHash = requestHash;
        this.inputHash = inputHash;
        return true;
    }

    public void markUncertain(final Instant now) {
        if (uncertainAt == null) {
            uncertainAt = now;
        }
    }

    public boolean requireReview() {
        if (reviewRequired) {
            return false;
        }
        reviewRequired = true;
        return true;
    }

    /** Keeps an anomalous past attempt available for review without blocking the current order. */
    public void isolateFromOrder() {
        blocksOrder = false;
    }

    public void resumeApprovalAfterReview() {
        if (completed || rejected || compensationRequested || !reviewRequired) {
            throw new CoreException(ErrorCode.PG_REQUEST_CONFLICT);
        }
        reviewRequired = false;
        uncertainAt = null;
    }

    /** Starts an explicitly authorized compensation and preserves its key across retries. */
    public void requestCompensation() {
        if (completed || rejected || type != PgOperationType.APPROVE || approvalAmount == null || approvalAmount.signum() <= 0
            || externalPaymentKey == null || pgOrderId == null || attemptId == null) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
        if (!compensationRequested) {
            compensationRequested = true;
            compensationIdempotencyKey = UUID.randomUUID().toString();
            compensationRequestedAt = Instant.now();
            uncertainAt = null;
            reviewRequired = false;
        } else if (reviewRequired) {
            // Explicit operator review resumes the same compensation, not a new PG operation.
            uncertainAt = null;
            reviewRequired = false;
        }
    }

    public void verifyRetryWindow() {
        if (rejected) {
            throw new CoreException(ErrorCode.PG_REJECTED);
        }
        // 토스의 15일 보관 경계보다 짧게 제한하여 만료된 키의 재실행을 방지한다.
        if (!completed && requestedAt.plus(14, ChronoUnit.DAYS).isBefore(Instant.now())) {
            throw new CoreException(ErrorCode.PG_REVIEW_REQUIRED);
        }
    }

    public void complete(final Long resultId) {
        if (resultId == null) {
            throw new IllegalArgumentException("처리 결과 ID가 필요합니다.");
        }
        this.resultId = resultId;
        this.completed = true;
    }

    public static String hash(final String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable");
        }
    }
}
