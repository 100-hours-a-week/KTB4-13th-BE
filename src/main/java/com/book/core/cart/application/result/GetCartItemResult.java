package com.book.core.cart.application.result;

import java.math.BigDecimal;

public record GetCartItemResult(Long cartItemId,Long productId,String itemName,String thumbnailUrl,BigDecimal salePrice,BigDecimal discountedPrice,Integer quantity,boolean isAvailableForPurchase){}
