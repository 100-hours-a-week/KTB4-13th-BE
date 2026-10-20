package com.book.core.agentchat.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 실행 가능 기간이 끝나도 제안과 표시 기준은 삭제하지 않습니다. */
@Entity
@Table(name = "cart_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CartProposal extends BaseEntity {
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(name = "source_key", nullable = false, updatable = false, length = 100)
    private String sourceKey;
    @Column(nullable = false, updatable = false, length = 2000)
    private String intent;
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean invalidated;
    @Column(name = "execution_id")
    private Long executionId;
    @ElementCollection
    @CollectionTable(name = "cart_proposal_items", joinColumns = @JoinColumn(name = "proposal_id"))
    @OrderBy("productId ASC")
    private List<CartChangeLine> items = new ArrayList<>();

    public CartProposal(final Long userId, final String sourceKey, final String intent, final List<CartChangeLine> items,
        final Instant createdAt) {
        super(null);
        if (userId == null || userId <= 0 || sourceKey == null || sourceKey.isBlank() || sourceKey.length() > 100 || intent == null
            || intent.isBlank() || intent.length() > 2000 || createdAt == null || items == null || items.isEmpty() || items.size() > 30
            || items.stream().anyMatch(Objects::isNull)
            || items.stream().map(CartChangeLine::productId).distinct().count() != items.size()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.sourceKey = sourceKey;
        this.intent = intent;
        this.items = new ArrayList<>(items);
        this.expiresAt = createdAt.plusSeconds(600);
    }

    public List<CartChangeLine> items() {
        return List.copyOf(items);
    }

    public void validateIntent(final String requestedIntent) {
        if (!intent.equals(requestedIntent)) {
            throw new CoreException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }

    public boolean executableAt(final Instant now) {
        return executionId == null && !invalidated && now.isBefore(expiresAt);
    }

    public String stateAt(final Instant now) {
        if (executionId != null) {
            return "EXECUTED";
        }
        if (invalidated) {
            return "INVALIDATED";
        }
        if (!now.isBefore(expiresAt)) {
            return "EXPIRED";
        }
        return "PENDING";
    }

    public void consume(final Long appliedExecutionId, final Instant now) {
        if (!executableAt(now) || appliedExecutionId == null) {
            throw new IllegalStateException("실행할 수 없는 제안입니다.");
        }
        executionId = appliedExecutionId;
    }

    public void invalidate() {
        if (executionId != null) {
            throw new IllegalStateException("이미 실행된 제안입니다.");
        }
        invalidated = true;
    }
}
