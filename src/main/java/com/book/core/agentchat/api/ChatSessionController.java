package com.book.core.agentchat.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.agentchat.api.converter.ChatSessionCommandConverter;
import com.book.core.agentchat.api.converter.ChatSessionResultConverter;
import com.book.core.agentchat.api.request.SendChatMessageRequest;
import com.book.core.agentchat.api.response.ChatHistoryResponse;
import com.book.core.agentchat.api.response.ChatSessionReplyResponse;
import com.book.core.agentchat.api.response.ChatSessionCreatedResponse;
import com.book.core.agentchat.api.response.ChatSessionListResponse;
import com.book.core.agentchat.api.spec.ChatSessionControllerSpec;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.result.ChatSessionPageResult;
import com.book.core.agentchat.application.service.ChatSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommend/chat/sessions")
class ChatSessionController implements ChatSessionControllerSpec {
    private static final CacheControl PRIVATE_NO_STORE = CacheControl.noStore().cachePrivate();

    private final ChatSessionService service;
    private final ChatSessionCommandConverter commandConverter;
    private final ChatSessionResultConverter resultConverter;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<ChatSessionCreatedResponse>> create(@UserId final Long userId) {
        final Long sessionId = service.create(userId);
        return ResponseEntity.created(URI.create("/api/v1/recommend/chat/sessions/" + sessionId)).cacheControl(PRIVATE_NO_STORE)
            .body(ApiResponse.ok(resultConverter.toCreatedResponse(sessionId)));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<ChatSessionListResponse>> list(@UserId final Long userId,
        @Positive @RequestParam(required = false) final Long beforeId,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") final int limit) {
        final ChatSessionPageResult result = service.list(commandConverter.toListCommand(userId, beforeId, limit));
        return ResponseEntity.ok().cacheControl(PRIVATE_NO_STORE).body(ApiResponse.ok(resultConverter.toListResponse(result)));
    }

    @Override
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> history(@UserId final Long userId, @Positive @PathVariable final Long sessionId,
        @PositiveOrZero @RequestParam(defaultValue = "0") final long afterSequence,
        @Min(1) @Max(100) @RequestParam(defaultValue = "50") final int limit) {
        final List<ChatHistoryMessage> result = service.history(commandConverter.toHistoryCommand(userId, sessionId, afterSequence, limit));
        return ResponseEntity.ok().cacheControl(PRIVATE_NO_STORE).body(ApiResponse.ok(resultConverter.toHistoryResponse(result)));
    }

    @Override
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<ChatSessionReplyResponse>> send(@UserId final Long userId,
        @Positive @PathVariable final Long sessionId, @Valid @RequestBody final SendChatMessageRequest request) {
        final ChatResponseSnapshot result = service.send(commandConverter.toSendCommand(userId, sessionId, request));
        return ResponseEntity.ok().cacheControl(PRIVATE_NO_STORE).body(ApiResponse.ok(resultConverter.toReplyResponse(result)));
    }
}
