package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ModifyCartItemUseCase {
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional
    public void execute(final ModifyCartItemCommand command) {
        final var cartItem = cartItemRepository
                .findActiveByUserIdAndId(command.userId(), command.cartItemId())
                .orElseThrow(() -> new CoreException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItem.applyQuantity(command.quantity());
    }
}
