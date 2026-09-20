package com.book.core.address.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Address extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String label;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String address;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    public Address(
            final Long id,
            final Long userId,
            final String label,
            final String postalCode,
            final String address,
            final String detailAddress,
            final boolean defaultAddress) {
        super(id);
        this.userId = userId;
        this.label = normalizeRequired(label);
        this.postalCode = normalizeRequired(postalCode);
        this.address = normalizeRequired(address);
        this.detailAddress = normalizeDetail(detailAddress);
        this.defaultAddress = defaultAddress;
    }

    public static Address of(
            final long userId,
            final String label,
            final String postalCode,
            final String address,
            final String detailAddress,
            final boolean defaultAddress) {
        return new Address(null, userId, label, postalCode, address, detailAddress, defaultAddress);
    }

    public boolean isDefault() {
        return defaultAddress;
    }

    public void makeDefault() {
        this.defaultAddress = true;
    }

    public void releaseDefault() {
        this.defaultAddress = false;
    }

    private static String normalizeRequired(final String value) {
        if (value == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private static String normalizeDetail(final String value) {
        if (value == null) {
            return null;
        }
        final String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }
}
