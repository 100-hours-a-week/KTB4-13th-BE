package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRestockRepositoryPort;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetReceivableRestocksUseCase {
    private static final Duration LEAD_TIME = Duration.ofHours(24);

    private final ProductRestockRepositoryPort restockRepository;

    @Transactional(readOnly = true)
    public List<Long> execute(final LocalDateTime now) {
        return restockRepository.findPendingIdsCreatedBefore(now.minus(LEAD_TIME));
    }
}
