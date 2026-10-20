package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRestockRepositoryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetRestockCandidatesUseCase {
    private final ProductRestockRepositoryPort restockRepository;

    @Transactional(readOnly = true)
    public List<Long> execute() {
        return restockRepository.findRestockCandidateProductIds();
    }
}
