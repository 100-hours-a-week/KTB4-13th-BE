package com.book.core.category.infrastructure.persistence.repository;

import com.book.core.category.application.port.CategoryRepositoryPort;
import com.book.core.category.domain.Category;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {
    private final CategoryJpaRepository jpaRepository;

    @Override
    public List<Category> findActive() {
        return jpaRepository.findByDeletedAtIsNull();
    }
}
