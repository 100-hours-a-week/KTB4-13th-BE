package com.book.core.merchant.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "merchant_product_mapping",
    indexes = @Index(name = "udx_merchant_product", columnList = "merchant_id, product_id", unique = true))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class MerchantProductMapping extends BaseEntity {
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    public MerchantProductMapping(final Long productId, final Long merchantId) {
        this.productId = productId;
        this.merchantId = merchantId;
    }
}
