package com.book.core.agentchat.api.converter;

import com.book.core.agentchat.api.request.ConfirmCartProposalRequest;
import com.book.core.agentchat.application.command.AgentCartResourceCommand;
import com.book.core.agentchat.application.command.ConfirmCartProposalCommand;
import org.springframework.stereotype.Component;

@Component
public class AgentCartCommandConverter {
    public ConfirmCartProposalCommand toConfirmCommand(final Long userId, final ConfirmCartProposalRequest request) {
        return new ConfirmCartProposalCommand(userId, request.proposalId(), request.idempotencyKey());
    }

    public AgentCartResourceCommand toResourceCommand(final Long userId, final Long resourceId) {
        return new AgentCartResourceCommand(userId, resourceId);
    }
}
