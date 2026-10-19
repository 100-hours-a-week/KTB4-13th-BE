package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteCartItemsUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional
    public void execute(final DeleteCartItemsCommand command) {
        final Cart cart =
            cartRepository.findByUserIdForChange(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.CART_ITEM_NOT_FOUND));
        final List<Long> cartItemIds = command.cartItemIds().stream().distinct().toList();
        final List<CartItem> cartItems = cartItemRepository.findAllByCartIdForChange(cart.id()).stream()
            .filter((final CartItem item) -> item.isActive() && cartItemIds.contains(item.id())).toList();
        if (cartItems.size() != cartItemIds.size()) {
            throw new CoreException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        cartItems.forEach(CartItem::delete);
    }
}
