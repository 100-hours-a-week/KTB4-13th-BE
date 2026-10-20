package com.book.core.agentchat.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 성공한 변경만 나타냅니다. 항목은 불변 요청의 항목을 참조합니다. */
@Entity
@Table(name = "cart_executions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CartExecution extends BaseEntity {
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @Column(name = "request_id", nullable = false, updatable = false)
    private Long requestId;
    @Column(name = "proposal_id", updatable = false)
    private Long proposalId;
    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    public CartExecution(final Long userId, final Long requestId, final Long proposalId, final Instant appliedAt) {
        super(null);
        if (userId == null || userId <= 0 || requestId == null || requestId <= 0 || appliedAt == null
            || (proposalId != null && proposalId <= 0)) {
            throw new IllegalArgumentException("유효한 사용자·원본 요청·성공 시각이 필요합니다.");
        }
        this.userId = userId;
        this.requestId = requestId;
        this.proposalId = proposalId;
        this.appliedAt = appliedAt;
    }
}
