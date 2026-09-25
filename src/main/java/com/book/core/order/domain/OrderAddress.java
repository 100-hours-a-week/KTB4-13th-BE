package com.book.core.order.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "order_addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OrderAddress {
    private static final int MAX_POSTAL_CODE_LENGTH = 5;
    private static final int MAX_ADDRESS_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "postal_code", nullable = false, length = MAX_POSTAL_CODE_LENGTH)
    private String postalCode;

    @Column(nullable = false, length = MAX_ADDRESS_LENGTH)
    private String address;

    @Column(name = "detail_address", length = MAX_ADDRESS_LENGTH)
    private String detailAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private OrderAddress(final Long id, final String postalCode, final String address, final String detailAddress) {
        if (postalCode == null || postalCode.isBlank() || postalCode.length() > MAX_POSTAL_CODE_LENGTH) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (address == null || address.isBlank() || address.length() > MAX_ADDRESS_LENGTH) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (detailAddress != null && detailAddress.length() > MAX_ADDRESS_LENGTH) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.id = id;
        this.postalCode = postalCode;
        this.address = address;
        this.detailAddress = detailAddress;
    }

    public static OrderAddress from(final String postalCode, final String address, final String detailAddress) {
        return new OrderAddress(null, postalCode, address, detailAddress);
    }
}
