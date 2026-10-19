package com.book.core.agentchat.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 제안·요청의 저장 항목입니다. 수량 허용 범위는 Cart 공개 입력 계약에서 검증합니다. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class CartChangeLine {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private CartChangeOperation operation;
    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;
    @Column(name = "expected_quantity", nullable = false, updatable = false)
    private int expectedQuantity;
    @Column(updatable = false)
    private Integer quantity;
    @Column(name = "expected_unit_price", precision = 19, scale = 2, updatable = false)
    private BigDecimal expectedUnitPrice;

    public CartChangeLine(final CartChangeOperation operation, final Long productId, final int expectedQuantity, final Integer quantity,
        final BigDecimal expectedUnitPrice) {
        if (operation == null || productId == null || productId <= 0 || expectedQuantity < 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (operation != CartChangeOperation.ADD && expectedQuantity == 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (operation == CartChangeOperation.REMOVE) {
            if (quantity != null || expectedUnitPrice != null) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            if (quantity == null || expectedUnitPrice == null || expectedUnitPrice.signum() < 0) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            try {
                this.expectedUnitPrice = expectedUnitPrice.setScale(2, RoundingMode.UNNECESSARY);
            } catch (final ArithmeticException exception) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
            if (this.expectedUnitPrice.precision() > 19) {
                throw new CoreException(ErrorCode.INVALID_REQUEST);
            }
        }
        this.operation = operation;
        this.productId = productId;
        this.expectedQuantity = expectedQuantity;
        this.quantity = quantity;
    }

}
