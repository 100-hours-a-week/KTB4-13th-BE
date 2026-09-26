package com.book.core.order.infrastructure.persistence.repository;

import com.book.core.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<Order, Long> {
}
