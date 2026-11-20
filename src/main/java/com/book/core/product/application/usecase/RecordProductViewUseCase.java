package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductViewRepositoryPort;
import com.book.core.product.domain.ProductView;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RecordProductViewUseCase {
    private final ProductViewRepositoryPort productViewRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(final Long userId, final Long productId) {
        productViewRepository.save(new ProductView(userId, productId, LocalDateTime.now(clock)));
    }
}
