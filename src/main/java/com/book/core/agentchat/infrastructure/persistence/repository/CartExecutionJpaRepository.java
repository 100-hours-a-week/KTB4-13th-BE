package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.domain.CartExecution;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartExecutionJpaRepository extends JpaRepository<CartExecution, Long> {
    Optional<CartExecution> findByUserIdAndId(final Long userId, final Long id);
}
