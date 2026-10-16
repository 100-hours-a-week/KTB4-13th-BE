package com.book.core.merchant.infrastructure.persistence.repository;

import com.book.core.merchant.domain.MerchantProductMapping;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface MerchantProductMappingJpaRepository extends JpaRepository<MerchantProductMapping, Long> {
    List<MerchantProductMapping> findByProductIdIn(final Collection<Long> productIds);
}
