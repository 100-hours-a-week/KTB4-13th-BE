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
