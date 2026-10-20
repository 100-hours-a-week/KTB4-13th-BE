package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.domain.CartProposal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CartProposalJpaRepository extends JpaRepository<CartProposal, Long> {
    Optional<CartProposal> findByUserIdAndId(final Long userId, final Long id);
}
