package com.book.core.agentchat.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(oneOf = {ChatSessionReplyResponse.class, AgentRecommendationReplyResponse.class, ShoppingChatReplyResponse.class})
public sealed interface ChatReplyResponse permits ChatSessionReplyResponse, AgentRecommendationReplyResponse, ShoppingChatReplyResponse {
}
