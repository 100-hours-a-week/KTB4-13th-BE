package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.application.usecase.FindProductsByIdsUseCase;
import com.book.core.product.domain.Product;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCartUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;
    private final FindProductsByIdsUseCase findProductsByIdsUseCase;

    @Transactional(readOnly = true)
    public GetCartResult execute(final Long userId) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return cartRepository.findByUserId(userId).map((final var cart) -> {
            final var cartItems = cartItemRepository.findActiveByCartId(cart.id());
            if (cartItems.isEmpty()) {
                return GetCartResult.empty();
            }
            final Map<Long, Product> productsById = findProductsByIdsUseCase.execute(cartItems.stream().map(CartItem::productId).toList())
                .stream().collect(Collectors.toMap(Product::id, Function.identity()));
            return GetCartResult
                .of(cartItems.stream().map((final var item) -> toResult(item, productsById.get(item.productId()))).toList());
        }).orElseGet(GetCartResult::empty);
    }

    private static GetCartItemResult toResult(final CartItem item, final Product product) {
        if (product == null) {
            return new GetCartItemResult(item.id(), item.productId(), null, null, null, null, item.quantity(), false);
        }
        if (!product.isActive() || !product.book().isActive()) {
            return new GetCartItemResult(item.id(), item.productId(), product.name(), product.thumbnailUrl(), null, null, item.quantity(),
                false);
        }
        return new GetCartItemResult(item.id(), item.productId(), product.name(), product.thumbnailUrl(), product.salePrice(),
            product.discountedPrice(), item.quantity(), product.stockQuantity() >= item.quantity());
    }
}
