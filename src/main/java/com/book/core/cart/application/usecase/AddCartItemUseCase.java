package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.CartItem;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AddCartItemUseCase {
    private final CartRepositoryPort cartRepository;
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional
    public void execute(final AddCartItemCommand command) {
        // 장바구니가 존재하지 않으면 예외 발생
        final var cart = cartRepository.findByUserId(command.userId())
            .orElseThrow(() -> new CoreException(ErrorCode.CART_NOT_FOUND));

        // 장바구니에 이미 존재하는 상품이면 수량을 대체하고, 존재하지 않으면 새로 추가
        final var cartItem = cartItemRepository.findByCartIdAndProductId(cart.id(), command.productId())
            .orElseGet(() -> cart.addItem(CartItem.from(cart.id(), command.productId(), command.quantity())));

        // 이미 삭제된 상품이면 활성화
        if (cartItem.isDeleted()) { cartItem.active(); }
        // 수량을 대체
        cartItem.applyQuantity(command.quantity());
    }
}
