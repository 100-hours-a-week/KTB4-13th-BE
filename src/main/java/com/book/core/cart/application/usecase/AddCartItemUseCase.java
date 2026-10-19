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
import java.util.Optional;
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
        // 장바구니가 존재하지 않으면 예외 발생
        final Cart cart =
            cartRepository.findByUserIdForChange(command.userId()).orElseThrow(() -> new CoreException(ErrorCode.CART_NOT_FOUND));

        // 장바구니에 이미 존재하는 상품이면 수량을 대체하고, 존재하지 않으면 새로 추가
        final List<CartItem> cartItems = cartItemRepository.findAllByCartIdForChange(cart.id());
        final Optional<CartItem> found =
            cartItems.stream().filter((final CartItem item) -> item.productId().equals(command.productId())).findFirst();
        CartItem.validateQuantity(command.quantity());
        productDetailUseCase.validateAvailableStock(command.productId(), command.quantity());

        // 이미 존재하는 상품인지 확인
        if (found.isPresent()) {
            // 이미 존재하는 상품이면
            final CartItem cartItem = found.get();
            // 이미 삭제된 상품이면 활성화
            if (cartItem.isDeleted()) {
                // 장바구니에 추가할 수 있는지 확인
                final int cartItemCount = (int) cartItems.stream().filter(CartItem::isActive).count();
                cart.validateCanAddItem(cartItemCount);
                cartItem.active(); // 상품을 활성화
            }
            // 수량을 대체
            cartItem.applyQuantity(command.quantity());
            return;
        }
        // 새로 추가하는 상품이면, 장바구니에 추가할 수 있는지 확인
        final int cartItemCount = (int) cartItems.stream().filter(CartItem::isActive).count();
        cart.validateCanAddItem(cartItemCount); // 장바구니에 추가할 수 있는지 확인
        final CartItem cartItem = CartItem.from(cart.id(), command.productId(), command.quantity());
        cartItemRepository.save(cartItem);
    }
}
