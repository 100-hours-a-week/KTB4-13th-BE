package com.book.core.product.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "product_restocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ProductRestock extends BaseEntity {
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductRestockStatus status;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    public ProductRestock(final Long productId, final int quantity) {
        if (productId == null || productId <= 0 || quantity <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.productId = productId;
        this.quantity = quantity;
        this.status = ProductRestockStatus.PENDING;
    }

    public void receive(final LocalDateTime receivedAt) {
        if (status != ProductRestockStatus.PENDING || receivedAt == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.status = ProductRestockStatus.COMPLETED;
        this.receivedAt = receivedAt;
    }
}
