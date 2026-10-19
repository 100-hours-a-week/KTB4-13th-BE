package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteCartItemUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional
    public void execute(final DeleteCartItemCommand command) {
        final Cart cart =
            cartRepository.findByUserIdForChange(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.findAllByCartIdForChange(cart.id()).stream()
            .filter((final CartItem item) -> item.isActive() && item.id().equals(command.cartItemId())).findFirst()
            .orElseThrow(() -> new CoreException(ErrorCode.CART_ITEM_NOT_FOUND)).delete();
    }
}
