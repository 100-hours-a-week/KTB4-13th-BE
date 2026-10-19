package com.book.core.agentchat.application.result;



public record AgentExecutionSummary(Long executionId, Long proposalId, String status, String rejectionCode) {
}
