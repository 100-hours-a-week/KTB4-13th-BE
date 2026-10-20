package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.port.CartItemRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCartUserIdsByProductIdUseCase {
    private final CartItemRepositoryPort cartItemRepository;

    @Transactional(readOnly = true)
    public List<Long> execute(final Long productId) {
        return cartItemRepository.findActiveUserIdsByProductId(productId);
    }
}
