package com.book.core.agentchat.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.agentchat.api.converter.AgentCartCommandConverter;
import com.book.core.agentchat.api.converter.AgentCartResultConverter;
import com.book.core.agentchat.api.request.ConfirmCartProposalRequest;
import com.book.core.agentchat.api.response.AgentCartExecutionResponse;
import com.book.core.agentchat.api.response.AgentCartProposalResponse;
import com.book.core.agentchat.api.spec.AgentCartControllerSpec;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.application.service.AgentCartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent/cart")
class AgentCartController implements AgentCartControllerSpec {
    private static final CacheControl PRIVATE_NO_STORE = CacheControl.noStore().cachePrivate();
    private final AgentCartService service;
    private final AgentCartCommandConverter commands;
    private final AgentCartResultConverter results;

    @Override
    @PostMapping("/executions")
    public ResponseEntity<ApiResponse<AgentCartExecutionResponse>> confirm(@UserId final Long userId,
        @Valid @RequestBody final ConfirmCartProposalRequest request) {
        final CartChangeResult result = service.confirm(commands.toConfirmCommand(userId, request));
        final URI location = URI.create("/api/v1/agent/cart/executions/" + result.executionId());
        if (result.replayed()) {
            return ResponseEntity.ok().cacheControl(PRIVATE_NO_STORE).header("Location", location.toString())
                .body(ApiResponse.ok(results.toExecutionResponse(result)));
        }
        return ResponseEntity.created(location).cacheControl(PRIVATE_NO_STORE).body(ApiResponse.ok(results.toExecutionResponse(result)));
    }

    @Override
    @GetMapping("/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<AgentCartProposalResponse>> proposal(@UserId final Long userId,
        @Positive @PathVariable final Long proposalId) {
        return ResponseEntity.ok().cacheControl(PRIVATE_NO_STORE)
            .body(ApiResponse.ok(results.toProposalResponse(service.getProposal(commands.toResourceCommand(userId, proposalId)))));
    }

    @Override
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ApiResponse<AgentCartExecutionResponse>> execution(@UserId final Long userId,
        @Positive @PathVariable final Long executionId) {
        return ResponseEntity.ok().cacheControl(PRIVATE_NO_STORE)
            .body(ApiResponse.ok(results.toExecutionResponse(service.getExecution(commands.toResourceCommand(userId, executionId)))));
    }
}
