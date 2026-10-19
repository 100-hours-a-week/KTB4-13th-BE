package com.book.core.agentchat.api.converter;

import com.book.core.agentchat.api.response.AgentRecommendationReplyResponse;
import com.book.core.agentchat.api.response.ChatCardResponse;
import com.book.core.agentchat.api.response.ChatHistoryMessageResponse;
import com.book.core.agentchat.api.response.ChatHistoryResponse;
import com.book.core.agentchat.api.response.ChatReplyResponse;
import com.book.core.agentchat.api.response.ChatSessionCreatedResponse;
import com.book.core.agentchat.api.response.ChatSessionListResponse;
import com.book.core.agentchat.api.response.ChatSessionReplyResponse;
import com.book.core.agentchat.api.response.ChatSessionSummaryResponse;
import com.book.core.agentchat.api.response.ShoppingChatReplyResponse;
import com.book.core.agentchat.application.result.ChatCardSnapshot;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.result.ChatSessionPageResult;
import com.book.core.agentchat.application.result.ChatSessionSummary;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionResultConverter {
    public ChatSessionCreatedResponse toCreatedResponse(final Long sessionId) {
        return new ChatSessionCreatedResponse(sessionId);
    }

    public ChatSessionListResponse toListResponse(final ChatSessionPageResult result) {
        return new ChatSessionListResponse(result.sessions().stream()
            .map((final ChatSessionSummary session) -> new ChatSessionSummaryResponse(session.sessionId(), session.createdAt())).toList(),
            result.nextBeforeId());
    }

    public ChatReplyResponse toReplyResponse(final ChatResponseSnapshot result) {
        if (result == null) {
            return null;
        }
        if (result.schemaVersion() == 2 && "shop".equals(result.route())) {
            return new ShoppingChatReplyResponse(result.route(), result.reply(), result.agentButtons(), result.execution());
        }
        final List<ChatCardResponse> cards = result.cards().stream()
            .map((final ChatCardSnapshot card) -> new ChatCardResponse(card.recommendationCardId(), card.bookId(), card.productId(),
                card.title(), card.author(), card.coverImageUrl(), card.price(), card.matchScore(), card.reasonShort(), card.reasonLong()))
            .toList();
        if (result.schemaVersion() == 2) {
            return new AgentRecommendationReplyResponse(result.route(), result.spec(), result.reply(), cards, result.followup(),
                result.agentButtons(), result.degraded());
        }
        return new ChatSessionReplyResponse(result.spec(), result.reply(), cards, result.followup(), result.buttons(), result.degraded());
    }

    public ChatHistoryResponse toHistoryResponse(final List<ChatHistoryMessage> messages) {
        Long nextAfterSequence = null;
        if (!messages.isEmpty()) {
            nextAfterSequence = messages.getLast().sequence();
        }
        return new ChatHistoryResponse(
            messages.stream().map((final ChatHistoryMessage message) -> new ChatHistoryMessageResponse(message.id(), message.sequence(),
                message.role(), message.text(), message.createdAt(), toReplyResponse(message.response()))).toList(),
            nextAfterSequence);
    }
}
