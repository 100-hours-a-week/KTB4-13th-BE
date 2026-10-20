package com.book.core.agentchat.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** READY는 재개 가능한 확정 후보입니다. 처리 중 상태를 미리 커밋하지 않고 결과와 Cart를 함께 확정합니다. */
@Entity
@Table(name = "cart_change_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CartChangeRequest extends BaseEntity {
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private CartChangeRequestKind kind;
    @Column(name = "request_key", nullable = false, updatable = false, length = 100)
    private String requestKey;
    @Column(nullable = false, updatable = false, length = 2000)
    private String intent;
    @Column(name = "proposal_id", updatable = false)
    private Long proposalId;
    @ElementCollection
    @CollectionTable(name = "cart_change_request_items", joinColumns = @JoinColumn(name = "request_id"))
    @OrderBy("productId ASC")
    private List<CartChangeLine> items = new ArrayList<>();
    @Column(name = "execution_id")
    private Long executionId;
    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    private ErrorCode rejection;

    public CartChangeRequest(final Long userId, final CartChangeRequestKind kind, final String requestKey, final String intent,
        final Long proposalId, final List<CartChangeLine> items) {
        super(null);
        if (userId == null || userId <= 0 || kind == null || requestKey == null || requestKey.isBlank() || requestKey.length() > 100
            || intent == null || intent.isBlank() || intent.length() > 2000 || items == null || items.isEmpty() || items.size() > 30
            || items.stream().anyMatch(Objects::isNull) || items.stream().map(CartChangeLine::productId).distinct().count() != items.size()
            || (kind == CartChangeRequestKind.DIRECT && proposalId != null)
            || (kind == CartChangeRequestKind.CONFIRMATION && (proposalId == null || proposalId <= 0))) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.kind = kind;
        this.requestKey = requestKey;
        this.intent = intent;
        this.proposalId = proposalId;
        this.items = new ArrayList<>(items);
    }

    public List<CartChangeLine> items() {
        return List.copyOf(items);
    }

    public void validateIntent(final String requestedIntent) {
        if (!intent.equals(requestedIntent)) {
            throw new CoreException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }

    public void apply(final Long appliedExecutionId) {
        if (executionId != null || rejection != null || appliedExecutionId == null) {
            throw new IllegalStateException("이미 완료됐거나 유효하지 않은 실행 결과입니다.");
        }
        executionId = appliedExecutionId;
    }

    public void reject(final ErrorCode reason) {
        if (executionId != null || rejection != null || reason == null) {
            throw new IllegalStateException("이미 완료됐거나 유효하지 않은 거절 결과입니다.");
        }
        rejection = reason;
    }
}
