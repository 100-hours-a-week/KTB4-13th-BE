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

    public static Address register(
            final long userId,
            final String label,
            final String postalCode,
            final String address,
            final String detailAddress) {
        return of(userId, label, postalCode, address, detailAddress, false);
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress() {
        this.defaultAddress = true;
    }

    public void unsetDefaultAddress() {
        this.defaultAddress = false;
    }

    public void updateDetailsFrom(final Address updatedAddress) {
        this.label = updatedAddress.label;
        this.postalCode = updatedAddress.postalCode;
        this.address = updatedAddress.address;
        this.detailAddress = updatedAddress.detailAddress;
    }

    private static String normalizeRequired(final String value) {
        if (value == null || value.isBlank()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return value.strip();
    }

    private static String normalizeDetail(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        final String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }
}
