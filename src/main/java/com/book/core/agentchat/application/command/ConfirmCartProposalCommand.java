package com.book.core.agentchat.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;

public record ConfirmCartProposalCommand(Long userId, Long proposalId, String idempotencyKey) {
    public ConfirmCartProposalCommand {
        if (userId == null || userId <= 0 || proposalId == null || proposalId <= 0 || idempotencyKey == null || idempotencyKey.isBlank()
            || idempotencyKey.length() > 100) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }
}
