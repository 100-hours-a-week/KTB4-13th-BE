package com.book.core.merchant.infrastructure.persistence.repository;

import com.book.core.merchant.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

interface MerchantJpaRepository extends JpaRepository<Merchant, Long> {
}
