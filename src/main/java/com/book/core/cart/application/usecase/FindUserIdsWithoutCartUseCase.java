package com.book.core.cart.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.cart.application.port.CartRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FindUserIdsWithoutCartUseCase {
    private final CartRepositoryPort cartRepository;

    @Transactional(readOnly = true)
    public List<Long> execute(final LocalDateTime registeredBefore, final Long afterUserId, final int limit) {
        return cartRepository.findActiveUserIdsWithoutCart(registeredBefore, afterUserId, limit);
    }
}
