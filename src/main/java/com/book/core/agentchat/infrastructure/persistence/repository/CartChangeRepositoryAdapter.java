package com.book.core.agentchat.infrastructure.persistence.repository;

import com.book.core.agentchat.application.port.CartChangeRepositoryPort;
import com.book.core.agentchat.domain.CartChangeRequest;
import com.book.core.agentchat.domain.CartChangeRequestKind;
import com.book.core.agentchat.domain.CartExecution;
import com.book.core.agentchat.domain.CartProposal;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartChangeRepositoryAdapter implements CartChangeRepositoryPort {
    private final CartChangeRequestJpaRepository requests;
    private final CartProposalJpaRepository proposals;
    private final CartExecutionJpaRepository executions;
    private final EntityManager entityManager;

    @Override
    public CartChangeRequest saveRequest(final CartChangeRequest request) {
        return requests.save(request);
    }

    @Override
    public CartProposal saveProposal(final CartProposal proposal) {
        return proposals.save(proposal);
    }

    @Override
    public CartExecution saveExecution(final CartExecution execution) {
        return executions.save(execution);
    }

    @Override
    public Optional<CartChangeRequest> findRequest(final Long userId, final Long requestId) {
        return requests.findByUserIdAndId(userId, requestId);
    }

    @Override
    public Optional<CartChangeRequest> findRequestForChange(final Long userId, final Long requestId) {
        return requestQuery("r.userId = :userId and r.id = :id").setParameter("userId", userId).setParameter("id", requestId)
            .uniqueResultOptional();
    }

    @Override
    public Optional<CartChangeRequest> findRequestByKeyForChange(final Long userId, final CartChangeRequestKind kind, final String key) {
        return requestQuery("r.userId = :userId and r.kind = :kind and r.requestKey = :key").setParameter("userId", userId)
            .setParameter("kind", kind).setParameter("key", key).uniqueResultOptional();
    }

    @Override
    public Optional<CartProposal> findProposalForChange(final Long userId, final Long proposalId) {
        return proposalQuery("p.userId = :userId and p.id = :id").setParameter("userId", userId).setParameter("id", proposalId)
            .uniqueResultOptional();
    }

    @Override
    public Optional<CartProposal> findProposalBySourceForChange(final Long userId, final String sourceKey) {
        return proposalQuery("p.userId = :userId and p.sourceKey = :key").setParameter("userId", userId).setParameter("key", sourceKey)
            .uniqueResultOptional();
    }

    @Override
    public Optional<CartProposal> findProposal(final Long userId, final Long proposalId) {
        return proposals.findByUserIdAndId(userId, proposalId);
    }

    @Override
    public Optional<CartExecution> findExecution(final Long userId, final Long executionId) {
        return executions.findByUserIdAndId(userId, executionId);
    }

    private SelectionQuery<CartChangeRequest> requestQuery(final String condition) {
        return entityManager.unwrap(Session.class)
            .createSelectionQuery("select r from CartChangeRequest r join fetch r.items where " + condition, CartChangeRequest.class);
    }

    private SelectionQuery<CartProposal> proposalQuery(final String condition) {
        return entityManager.unwrap(Session.class)
            .createSelectionQuery("select p from CartProposal p join fetch p.items where " + condition, CartProposal.class);
    }
}
