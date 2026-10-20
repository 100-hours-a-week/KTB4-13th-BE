package com.book.core.cart.application.usecase;

import java.time.Instant;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.ApplyCartChangesCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.CartBatchChangeResult;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.application.command.CartItemChange;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.application.result.CartChangeProductResult;
import com.book.core.product.application.usecase.GetProductsForCartChangeUseCase;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 현재 기준을 비교하고 여러 상품의 변경을 전체 적용하거나 전체 미적용합니다. */
@UseCase
@RequiredArgsConstructor
public class ApplyCartChangesUseCase {
    private final CartRepositoryPort carts;
    private final CartItemRepositoryPort items;
    private final GetProductsForCartChangeUseCase products;
    private final Clock clock;

    /** 관련 기록 조회보다 먼저 Cart 버전을 등록하고 자식만 바뀌어도 커밋에서 검사합니다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void verifyForChange(final Long userId) {
        cartForChange(userId);
    }

    @Transactional
    public CartBatchChangeResult execute(final ApplyCartChangesCommand command) {
        final Cart cart = cartForChange(command.userId());
        final Map<Long, CartItem> currentItems =
            items.findAllByCartIdForChange(cart.id()).stream().collect(Collectors.toMap(CartItem::productId, Function.identity()));
        final List<Long> productIds =
            command.items().stream().filter((final CartItemChange line) -> !line.isRemoval()).map(CartItemChange::productId).toList();
        final Map<Long, CartChangeProductResult> currentProducts;
        if (productIds.isEmpty()) {
            currentProducts = Map.of();
        } else {
            currentProducts = products.execute(productIds);
        }
        final Instant now = clock.instant();
        // 조회 이후 실제 변경 직전에 유효 기한을 확인합니다.
        if (command.validUntil() != null && !now.isBefore(command.validUntil())) {
            return new CartBatchChangeResult(null, null, true);
        }
        final ErrorCode rejection = validate(cart, command.items(), currentItems, currentProducts);
        if (rejection != null) {
            return new CartBatchChangeResult(null, rejection, false);
        }
        for (final CartItemChange line : command.items()) {
            final CartItem current = currentItems.get(line.productId());
            if (line.isRemoval()) {
                current.delete();
            } else if (current == null) {
                items.save(CartItem.from(cart.id(), line.productId(), line.quantity()));
            } else {
                current.active();
                current.applyQuantity(line.quantity());
            }
        }
        return new CartBatchChangeResult(now, null, false);
    }

    private ErrorCode validate(final Cart cart, final List<CartItemChange> lines, final Map<Long, CartItem> currentItems,
        final Map<Long, CartChangeProductResult> currentProducts) {
        int finalCount = (int) currentItems.values().stream().filter(CartItem::isActive).count();
        for (final CartItemChange line : lines) {
            final CartItem current = currentItems.get(line.productId());
            int quantity = 0;
            if (current != null && current.isActive()) {
                quantity = current.quantity();
            }
            if (quantity != line.expectedQuantity()) {
                return ErrorCode.CART_STATE_CHANGED;
            }
            if (line.isRemoval()) {
                finalCount--;
                continue;
            }
            final CartChangeProductResult product = currentProducts.get(line.productId());
            if (product == null || !product.active()) {
                return ErrorCode.PRODUCT_NOT_FOUND;
            }
            if (product.unitPrice().compareTo(line.expectedUnitPrice()) != 0) {
                return ErrorCode.CART_PRICE_CHANGED;
            }
            if (product.stockQuantity() < line.quantity()) {
                return ErrorCode.INSUFFICIENT_PRODUCT_STOCK;
            }
            if (quantity == 0) {
                finalCount++;
            }
        }
        if (finalCount > 0) {
            try {
                cart.validateCanAddItem(finalCount - 1);
            } catch (final CoreException exception) {
                if (exception.errorCode() == ErrorCode.CART_ITEM_LIMIT_EXCEEDED) {
                    return exception.errorCode();
                }
                throw exception;
            }
        }
        return null;
    }

    private Cart cartForChange(final Long userId) {
        return carts.findByUserIdForChange(userId).orElseThrow(() -> new CoreException(ErrorCode.CART_NOT_FOUND));
    }
}
