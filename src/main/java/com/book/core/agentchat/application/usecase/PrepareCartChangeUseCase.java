package com.book.core.agentchat.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.PrepareCartChangeCommand;
import com.book.core.agentchat.application.port.CartChangeRepositoryPort;
import com.book.core.agentchat.domain.CartChangeRequest;
import com.book.core.agentchat.domain.CartChangeRequestKind;
import com.book.core.agentchat.domain.CartProposal;
import com.book.core.cart.application.usecase.ApplyCartChangesUseCase;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class PrepareCartChangeUseCase {
    private final ApplyCartChangesUseCase cartChanges;
    private final CartChangeRepositoryPort changes;
    private final Clock clock;

    /** BE가 검증한 직접 지시의 최초 기준을 저장하거나 동일 요청을 복원합니다. */
    @Transactional
    public Long prepareDirect(final PrepareCartChangeCommand command) {
        if (changes.findProposalBySourceForChange(command.userId(), command.requestKey()).isPresent()) {
            throw new CoreException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        final Optional<CartChangeRequest> existing =
            changes.findRequestByKeyForChange(command.userId(), CartChangeRequestKind.DIRECT, command.requestKey());
        if (existing.isPresent()) {
            existing.get().validateIntent(command.intent());
            return existing.get().id();
        }
        cartChanges.verifyForChange(command.userId());
        return changes.saveRequest(new CartChangeRequest(command.userId(), CartChangeRequestKind.DIRECT, command.requestKey(),
            command.intent(), null, command.items())).id();
    }

    /** 표시할 제안은 같은 source에서 한 번만 생성하며 기준·만료를 갱신하지 않습니다. */
    @Transactional
    public Long createProposal(final PrepareCartChangeCommand command) {
        if (changes.findRequestByKeyForChange(command.userId(), CartChangeRequestKind.DIRECT, command.requestKey()).isPresent()) {
            throw new CoreException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        final Optional<CartProposal> existing = changes.findProposalBySourceForChange(command.userId(), command.requestKey());
        if (existing.isPresent()) {
            existing.get().validateIntent(command.intent());
            return existing.get().id();
        }
        cartChanges.verifyForChange(command.userId());
        return changes
            .saveProposal(new CartProposal(command.userId(), command.requestKey(), command.intent(), command.items(), clock.instant()))
            .id();
    }

}
