package com.book.core.agentchat.application.port;

import com.book.core.agentchat.domain.CartChangeRequest;
import com.book.core.agentchat.domain.CartChangeRequestKind;
import com.book.core.agentchat.domain.CartExecution;
import com.book.core.agentchat.domain.CartProposal;
import java.util.Optional;

/** 변경용 조회는 호출자가 Cart 공개 UseCase로 사용자 Cart 버전 검사를 등록한 트랜잭션에서 사용합니다. */
public interface CartChangeRepositoryPort {
    CartChangeRequest saveRequest(final CartChangeRequest request);

    CartProposal saveProposal(final CartProposal proposal);

    CartExecution saveExecution(final CartExecution execution);

    Optional<CartChangeRequest> findRequest(final Long userId, final Long requestId);

    Optional<CartChangeRequest> findRequestForChange(final Long userId, final Long requestId);

    Optional<CartChangeRequest> findRequestByKeyForChange(final Long userId, final CartChangeRequestKind kind, final String key);

    Optional<CartProposal> findProposalForChange(final Long userId, final Long proposalId);

    Optional<CartProposal> findProposalBySourceForChange(final Long userId, final String sourceKey);

    Optional<CartProposal> findProposal(final Long userId, final Long proposalId);

    Optional<CartExecution> findExecution(final Long userId, final Long executionId);

}
