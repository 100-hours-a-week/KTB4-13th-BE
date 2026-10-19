package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.PrepareCartChangeCommand;
import com.book.core.agentchat.application.port.CartChangeRepositoryPort;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.domain.CartChangeRequest;
import com.book.core.agentchat.domain.CartChangeRequestKind;
import com.book.core.agentchat.domain.CartExecution;
import com.book.core.agentchat.domain.CartProposal;
import com.book.core.cart.application.command.ApplyCartChangesCommand;
import com.book.core.cart.application.result.CartBatchChangeResult;
import com.book.core.cart.application.usecase.ApplyCartChangesUseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ExecuteCartChangeUseCase {
    private final ApplyCartChangesUseCase cartChanges;
    private final CartChangeRepositoryPort changes;
    private final Clock clock;

    /** 저장된 직접 지시를 실행하며 성공·거절은 같은 요청에 재생합니다. */
    @Transactional
    public CartChangeResult execute(final Long userId, final Long requestId) {
        final Optional<CartChangeRequest> found = changes.findRequestForChange(userId, requestId);
        if (found.isEmpty()) {
            cartChanges.verifyForChange(userId);
            throw new CoreException(ErrorCode.CART_CHANGE_NOT_FOUND);
        }
        final CartChangeRequest request = found.get();
        if (request.kind() != CartChangeRequestKind.DIRECT) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final CartChangeResult completed = completed(request);
        if (completed != null) {
            return completed;
        }
        cartChanges.verifyForChange(userId);
        return apply(request, null);
    }

    /** 사용자 승인 경로는 제안 ID와 키만 받으며 변경 항목을 다시 입력받지 않습니다. */
    @Transactional
    public CartChangeResult confirm(final Long userId, final Long proposalId, final String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100 || proposalId == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final String intent = "proposal:" + proposalId;
        final Optional<CartChangeRequest> existing =
            changes.findRequestByKeyForChange(userId, CartChangeRequestKind.CONFIRMATION, idempotencyKey);
        if (existing.isPresent()) {
            final CartChangeRequest request = existing.get();
            request.validateIntent(intent);
            final CartChangeResult completed = completed(request);
            if (completed != null) {
                return completed;
            }
        }
        cartChanges.verifyForChange(userId);
        final CartProposal proposal =
            changes.findProposalForChange(userId, proposalId).orElseThrow(() -> new CoreException(ErrorCode.CART_CHANGE_NOT_FOUND));
        final CartChangeRequest request;
        if (existing.isPresent()) {
            request = existing.get();
        } else {
            request = changes.saveRequest(
                new CartChangeRequest(userId, CartChangeRequestKind.CONFIRMATION, idempotencyKey, intent, proposalId, proposal.items()));
        }
        // 이미 성공했다면 새 키이거나 만료 시각이 지난 경우에도 기존 결과를 우선 복원합니다.
        if (proposal.executionId() != null) {
            request.apply(proposal.executionId());
            return appliedResult(userId, proposal.executionId(), true);
        }
        if (!proposal.executableAt(clock.instant())) {
            return reject(request, ErrorCode.PROPOSAL_NOT_EXECUTABLE, false);
        }
        return apply(request, proposal);
    }

    private CartChangeResult apply(final CartChangeRequest request, final CartProposal proposal) {
        final CartChangeResult completed = completed(request);
        if (completed != null) {
            return completed;
        }
        Instant validUntil = null;
        if (proposal != null) {
            validUntil = proposal.expiresAt();
        }
        final CartBatchChangeResult result = cartChanges
            .execute(new ApplyCartChangesCommand(request.userId(), PrepareCartChangeCommand.toCartItems(request.items()), validUntil));
        if (result.expired()) {
            return reject(request, ErrorCode.PROPOSAL_NOT_EXECUTABLE, false);
        }
        if (result.rejection() != null) {
            if (proposal != null) {
                proposal.invalidate();
            }
            return reject(request, result.rejection(), false);
        }
        final CartExecution execution =
            changes.saveExecution(new CartExecution(request.userId(), request.id(), request.proposalId(), result.appliedAt()));
        request.apply(execution.id());
        if (proposal != null) {
            proposal.consume(execution.id(), result.appliedAt());
        }
        return new CartChangeResult(execution.id(), request.proposalId(), request.items(), execution.appliedAt(), null, false);
    }

    private CartChangeResult completed(final CartChangeRequest request) {
        if (request.executionId() != null) {
            return appliedResult(request.userId(), request.executionId(), true);
        }
        if (request.rejection() != null) {
            return CartChangeResult.rejected(request.proposalId(), request.rejection(), true);
        }
        return null;
    }

    private CartChangeResult appliedResult(final Long userId, final Long executionId, final boolean replayed) {
        final CartExecution execution =
            changes.findExecution(userId, executionId).orElseThrow(() -> new IllegalStateException("완료된 성공 실행이 없습니다."));
        final CartChangeRequest original =
            changes.findRequestForChange(userId, execution.requestId()).orElseThrow(() -> new IllegalStateException("성공 실행의 원본 요청이 없습니다."));
        return new CartChangeResult(execution.id(), execution.proposalId(), original.items(), execution.appliedAt(), null, replayed);
    }

    private CartChangeResult reject(final CartChangeRequest request, final ErrorCode reason, final boolean replayed) {
        request.reject(reason);
        return CartChangeResult.rejected(request.proposalId(), reason, replayed);
    }
}
