package com.book.core.product.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.product.application.port.ProductRepositoryPort;
import com.book.core.product.domain.Product;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class FindProductByBookIdUseCase {
    private final ProductRepositoryPort productRepository;

    @Transactional(readOnly = true)
    public Optional<Product> execute(final Long bookId) {
        return productRepository.findActiveByBookId(bookId);
    }
}
