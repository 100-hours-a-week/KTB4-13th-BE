package com.book.core.agentchat.application.service;

import com.book.common.exception.CoreException;
import com.book.core.agentchat.application.command.AgentCartResourceCommand;
import com.book.core.agentchat.application.command.ConfirmCartProposalCommand;
import com.book.core.agentchat.application.command.PrepareCartChangeCommand;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.application.result.CartProposalResult;
import com.book.core.agentchat.application.usecase.ExecuteCartChangeUseCase;
import com.book.core.agentchat.application.usecase.GetCartExecutionUseCase;
import com.book.core.agentchat.application.usecase.GetCartProposalUseCase;
import com.book.core.agentchat.application.usecase.PrepareCartChangeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 외부 연동 전 Cart 실행 기반의 내부 진입점입니다. 인증·AI 후보 변환은 호출 경계가 담당합니다. */
@Service
@RequiredArgsConstructor
public class AgentCartService {
    private final PrepareCartChangeUseCase prepare;
    private final ExecuteCartChangeUseCase execute;
    private final GetCartProposalUseCase getProposal;
    private final GetCartExecutionUseCase getExecution;

    public CartChangeResult executeDirect(final PrepareCartChangeCommand command) {
        return execute.execute(command.userId(), prepare.prepareDirect(command));
    }

    public Long propose(final PrepareCartChangeCommand command) {
        return prepare.createProposal(command);
    }

    public CartChangeResult confirm(final ConfirmCartProposalCommand command) {
        final CartChangeResult result = execute.confirm(command.userId(), command.proposalId(), command.idempotencyKey());
        if (result.rejection() != null) {
            throw new CoreException(result.rejection());
        }
        return result;
    }

    public CartProposalResult getProposal(final AgentCartResourceCommand command) {
        return getProposal.execute(command.userId(), command.resourceId());
    }

    public CartChangeResult getExecution(final AgentCartResourceCommand command) {
        return getExecution.execute(command.userId(), command.resourceId());
    }
}
