package com.book.core.product.application.result;

import com.book.core.product.domain.Product;
import java.math.BigDecimal;

public record ProductListItem(Product product,Long salesQuantity,Long reviewCount,BigDecimal reviewRate){}
