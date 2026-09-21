package com.book.core.category.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.category.domain.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryJpaRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByStatus(final EntityStatus status);
}
