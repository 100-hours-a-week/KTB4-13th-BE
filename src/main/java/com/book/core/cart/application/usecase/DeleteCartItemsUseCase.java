package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteCartItemsUseCase {
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional
    public void execute(final DeleteCartItemsCommand command) {
        final var cartItemIds = command.cartItemIds().stream().distinct().toList();
        final var cartItems = cartItemRepository.findActiveByUserIdAndIds(command.userId(), cartItemIds);
        if (cartItems.size() != cartItemIds.size()) {
            throw new CoreException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        cartItems.forEach(item -> item.delete());
    }
}
