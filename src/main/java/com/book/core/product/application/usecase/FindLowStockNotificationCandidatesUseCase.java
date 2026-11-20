package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.port.ProductViewRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FindLowStockNotificationCandidatesUseCase {
    private final ProductViewRepositoryPort productViewRepository;

    @Transactional(readOnly = true)
    public List<Long> execute(final Long productId, final LocalDateTime referenceTime) {
        if (productId == null || productId <= 0 || referenceTime == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return productViewRepository.findLowStockNotificationCandidateUserIds(productId, referenceTime.minusDays(7), referenceTime);
    }
}
