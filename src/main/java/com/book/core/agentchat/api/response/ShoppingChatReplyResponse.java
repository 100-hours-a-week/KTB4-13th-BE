package com.book.core.agentchat.api.response;

import com.book.core.agentchat.application.result.AgentButton;
import com.book.core.agentchat.application.result.AgentCheckout;
import io.swagger.v3.oas.annotations.media.Schema;
import com.book.core.agentchat.application.result.AgentExecutionSummary;
import java.util.List;

public record ShoppingChatReplyResponse(String route, String reply, List<AgentButton> buttons, AgentExecutionSummary execution,
    @Schema(description = "담기 성공 시 주문 화면에 채울 검증된 쿠폰·포인트 값", nullable = true) AgentCheckout checkout) implements ChatReplyResponse {
}
