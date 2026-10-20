package com.book.core.agentchat.api.converter;

import com.book.core.agentchat.api.response.AgentCartExecutionResponse;
import com.book.core.agentchat.api.response.AgentCartItemResponse;
import com.book.core.agentchat.api.response.AgentCartProposalResponse;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.application.result.CartProposalResult;
import com.book.core.agentchat.domain.CartChangeLine;
import com.book.core.agentchat.domain.CartChangeOperation;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgentCartResultConverter {
    public AgentCartExecutionResponse toExecutionResponse(final CartChangeResult result) {
        return new AgentCartExecutionResponse(result.executionId(), result.proposalId(), result.appliedAt(), items(result.items()),
            Map.of("self", "/api/v1/agent/cart/executions/" + result.executionId(), "cart", "/api/v1/cart"));
    }

    public AgentCartProposalResponse toProposalResponse(final CartProposalResult result) {
        return new AgentCartProposalResponse(result.proposalId(), result.status().toLowerCase(Locale.ROOT), result.expiresAt(),
            items(result.items()), result.executionId(),
            Map.of("self", "/api/v1/agent/cart/proposals/" + result.proposalId(), "executions", "/api/v1/agent/cart/executions"));
    }

    private List<AgentCartItemResponse> items(final List<CartChangeLine> items) {
        return items.stream().map((final CartChangeLine item) -> {
            String operation = "set";
            if (item.operation() == CartChangeOperation.REMOVE) {
                operation = "remove";
            }
            return new AgentCartItemResponse(item.productId(), operation, item.quantity(), item.expectedQuantity(),
                item.expectedUnitPrice());
        }).toList();
    }
}
