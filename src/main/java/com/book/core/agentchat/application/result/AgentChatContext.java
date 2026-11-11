package com.book.core.agentchat.application.result;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.result.OwnedCouponResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public record AgentChatContext(List<AgentObservedProduct> products, List<AgentContextCard> cards, List<OwnedCouponResult> ownedCoupons,
    BigDecimal pointBalance) {
    public AgentChatContext {
        products = List.copyOf(products);
        cards = List.copyOf(cards);
        ownedCoupons = ownedCoupons == null ? List.of() : List.copyOf(ownedCoupons);
        pointBalance = pointBalance == null ? BigDecimal.ZERO : pointBalance.setScale(0, RoundingMode.DOWN);
    }

    public AgentObservedProduct requireProduct(final String isbn) {
        return findProduct(isbn).orElseThrow(() -> new CoreException(ErrorCode.AI_SERVICE_FAILURE));
    }

    public Optional<AgentObservedProduct> findProduct(final String isbn) {
        final List<AgentObservedProduct> found =
            products.stream().filter((final AgentObservedProduct product) -> isbn != null && isbn.equals(product.isbn())).toList();
        if (found.size() > 1) {
            throw new CoreException(ErrorCode.AI_SERVICE_FAILURE);
        }
        return found.stream().findFirst();
    }
}
