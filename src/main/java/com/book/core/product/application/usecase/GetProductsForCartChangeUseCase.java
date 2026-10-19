package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.application.port.CartChangeProductRepositoryPort;
import com.book.core.product.application.result.CartChangeProductResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetProductsForCartChangeUseCase {
    private final CartChangeProductRepositoryPort productRepository;

    /** 호출자의 장바구니 쓰기 트랜잭션에 참여하며, 없는 상품은 결과에서 제외합니다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, CartChangeProductResult> execute(final List<Long> productIds) {
        if (productIds == null || productIds.stream().anyMatch((final Long id) -> id == null || id <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return productRepository.findByIdsForChange(productIds);
    }
}
