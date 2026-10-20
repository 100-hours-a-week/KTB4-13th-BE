package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.port.CartChangeRepositoryPort;
import com.book.core.agentchat.application.result.CartProposalResult;
import com.book.core.agentchat.domain.CartProposal;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCartProposalUseCase {
    private final CartChangeRepositoryPort changes;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CartProposalResult execute(final Long userId, final Long proposalId) {
        final CartProposal proposal =
            changes.findProposal(userId, proposalId).orElseThrow(() -> new CoreException(ErrorCode.CART_CHANGE_NOT_FOUND));
        return new CartProposalResult(proposal.id(), proposal.stateAt(clock.instant()), proposal.expiresAt(), proposal.items(),
            proposal.executionId());
    }
}
