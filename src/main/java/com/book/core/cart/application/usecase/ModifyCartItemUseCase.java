package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ModifyCartItemUseCase {
    private final CartItemRepositoryPort cartItemRepository;
    private final GetProductDetailUseCase productDetailUseCase;

    @Transactional
    public void execute(final ModifyCartItemCommand command) {
        final CartItem cartItem = cartItemRepository
                .findActiveByUserIdAndId(command.userId(), command.cartItemId())
                .orElseThrow(() -> new CoreException(ErrorCode.CART_ITEM_NOT_FOUND));
        CartItem.validateQuantity(command.quantity());
        productDetailUseCase.validateAvailableStock(cartItem.productId(), command.quantity());
        cartItem.applyQuantity(command.quantity());
    }
}
