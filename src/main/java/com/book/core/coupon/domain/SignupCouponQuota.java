package com.book.core.coupon.domain;

import com.book.common.domain.BaseTimeEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "signup_coupon_quota")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class SignupCouponQuota extends BaseTimeEntity {
    @Id
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "issue_limit", nullable = false)
    private int issueLimit;

    @Column(name = "issued_count", nullable = false)
    private int issuedCount;

    @Version
    private long version;

    public boolean canIssue() {
        return issuedCount < issueLimit;
    }

    public void issue() {
        if (!canIssue()) {
            throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
        }
        issuedCount++;
    }
}
