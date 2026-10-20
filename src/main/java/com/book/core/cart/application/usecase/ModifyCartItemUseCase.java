package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ModifyCartItemUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;
    private final GetProductDetailUseCase productDetailUseCase;

    @Transactional
    public void execute(final ModifyCartItemCommand command) {
        final Cart cart =
            cartRepository.findByUserIdForChange(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.CART_ITEM_NOT_FOUND));
        final CartItem cartItem = cartItemRepository.findAllByCartIdForChange(cart.id()).stream()
            .filter((final CartItem item) -> item.isActive() && item.id().equals(command.cartItemId())).findFirst()
            .orElseThrow(() -> new CoreException(ErrorCode.CART_ITEM_NOT_FOUND));
        CartItem.validateQuantity(command.quantity());
        productDetailUseCase.validateAvailableStock(cartItem.productId(), command.quantity());
        cartItem.applyQuantity(command.quantity());
    }
}
