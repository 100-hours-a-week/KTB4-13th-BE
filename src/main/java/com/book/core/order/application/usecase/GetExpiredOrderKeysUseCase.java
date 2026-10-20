package com.book.core.order.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.order.application.port.OrderRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetExpiredOrderKeysUseCase {
    private static final int LIMIT = 100;

    private final OrderRepositoryPort orderRepository;

    @Transactional(readOnly = true)
    public List<String> execute(final LocalDateTime now) {
        return orderRepository.findExpiredOrderKeys(now, LIMIT);
    }
}
