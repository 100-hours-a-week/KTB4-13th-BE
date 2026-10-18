package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.domain.CartChangeRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartChangeRequestJpaRepository extends JpaRepository<CartChangeRequest, Long> {
    Optional<CartChangeRequest> findByUserIdAndId(final Long userId, final Long id);
}
