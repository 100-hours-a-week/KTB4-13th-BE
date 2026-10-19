package com.book.core.agentchat.application.result;

import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;

public record ChatAttemptResult(Long requestId, long generation, ChatRecommendationCommand aiCommand,
    ChatResponseSnapshot completedResponse, ErrorCode failure, Long retryAfterSeconds, boolean agentPrepared) {
}
