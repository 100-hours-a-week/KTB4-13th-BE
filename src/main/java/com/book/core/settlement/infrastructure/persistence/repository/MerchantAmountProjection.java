package com.book.core.settlement.infrastructure.persistence.repository;

import java.math.BigDecimal;

interface MerchantAmountProjection {
    Long getMerchantId();

    BigDecimal getAmount();
}
