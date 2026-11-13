package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.domain.CartItem;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AddCartItemUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;
    private final GetProductDetailUseCase productDetailUseCase;

    @Transactional
    public void execute(final AddCartItemCommand command) {
        final Cart cart =
            cartRepository.findByUserIdForChange(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.CART_NOT_FOUND));
        final List<CartItem> cartItems = cartItemRepository.findAllByCartIdForChange(cart.id());
        final CartItem existingItem =
            cartItems.stream().filter((final CartItem item) -> item.productId().equals(command.productId())).findFirst().orElse(null);

        final int requestedQuantity = command.quantity();
        CartItem.validateQuantity(requestedQuantity);
        final boolean alreadyActive = existingItem != null && existingItem.isActive();
        final int totalQuantity = alreadyActive ? existingItem.quantity() + requestedQuantity : requestedQuantity;
        CartItem.validateQuantity(totalQuantity);
        productDetailUseCase.validateAvailableStock(command.productId(), totalQuantity);

        if (!alreadyActive) {
            final int activeItemCount = (int) cartItems.stream().filter(CartItem::isActive).count();
            cart.validateCanAddItem(activeItemCount);
        }
        if (existingItem == null) {
            cartItemRepository.save(CartItem.from(cart.id(), command.productId(), totalQuantity));
            return;
        }

        if (existingItem.isDeleted()) {
            existingItem.active();
        }
        existingItem.applyQuantity(totalQuantity);
    }
}
