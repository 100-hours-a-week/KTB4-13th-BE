package com.book.core.coupon.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "signup_coupon_receipt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class SignupCouponReceipt extends BaseEntity {
    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "provider_email", nullable = false, length = 254)
    private String providerEmail;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public SignupCouponReceipt(final Long couponId, final String providerEmail, final Long userId, final LocalDateTime issuedAt) {
        this.couponId = couponId;
        this.providerEmail = normalize(providerEmail);
        this.userId = userId;
        this.issuedAt = issuedAt;
        validate();
    }

    public boolean isHeld(final LocalDateTime now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }

    public void reissue(final Long userId, final LocalDateTime issuedAt) {
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = null;
    }

    public void retainUntil(final LocalDateTime expiresAt) {
        if (expiresAt == null || expiresAt.isBefore(issuedAt)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.expiresAt = expiresAt;
        userId = null;
    }

    private void validate() {
        if (couponId == null || couponId <= 0 || providerEmail == null || providerEmail.isBlank() || providerEmail.length() > 254
            || userId == null || userId <= 0 || issuedAt == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static String normalize(final String email) {
        return email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
    }
}
