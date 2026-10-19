package com.book.core.agentchat.application.result;

import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.domain.CartChangeLine;
import java.time.Instant;
import java.util.List;

public record CartChangeResult(Long executionId, Long proposalId, List<CartChangeLine> items, Instant appliedAt, ErrorCode rejection,
    boolean replayed) {
    public CartChangeResult {
        items = List.copyOf(items);
    }

    public static CartChangeResult rejected(final Long proposalId, final ErrorCode reason, final boolean replayed) {
        return new CartChangeResult(null, proposalId, List.of(), null, reason, replayed);
    }
}
