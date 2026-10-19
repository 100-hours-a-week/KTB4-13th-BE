package com.book.core.agentchat.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.agentchat.api.request.SendChatMessageRequest;
import com.book.core.agentchat.api.response.ChatHistoryResponse;
import com.book.core.agentchat.api.response.ChatSessionReplyResponse;
import com.book.core.agentchat.api.response.ChatSessionCreatedResponse;
import com.book.core.agentchat.api.response.ChatSessionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

@Tag(name = "대화방", description = "인증 사용자별 추천 대화방과 저장 이력")
@SecurityRequirement(name = "bearerAuth")
public interface ChatSessionControllerSpec {
    @Operation(summary = "빈 대화방 생성", description = "201과 새 대화방 식별자를 반환합니다. 사용자 ID는 인증에서만 추출합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "대화방 생성, Location은 새 대화방 식별 URI"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")})
    ResponseEntity<ApiResponse<ChatSessionCreatedResponse>> create(@Parameter(hidden = true) Long userId);

    @Operation(summary = "내 대화방 목록", description = "생성 ID 내림차순. nextBeforeId를 다음 beforeId로 사용하며 null이면 마지막입니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자별 목록"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 커서 또는 크기"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")})
    ResponseEntity<ApiResponse<ChatSessionListResponse>> list(@Parameter(hidden = true) Long userId, @Positive Long beforeId,
        @Min(1) @Max(100) int limit);

    @Operation(summary = "대화 이력 조회", description = "순서 오름차순, 당시 응답 복원. nextAfterSequence로 다음 페이지를 조회하며 빈 목록이면 현재 끝입니다. 타인 방은 404입니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 이력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 순서 또는 크기"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 타인 대화방")})
    ResponseEntity<ApiResponse<ChatHistoryResponse>> history(@Parameter(hidden = true) Long userId, @Positive Long sessionId,
        @PositiveOrZero long afterSequence, @Min(1) @Max(100) int limit);

    @Operation(summary = "대화 전송",
        description = "저장한 최근 20개로 기존 AI 추천을 호출합니다. " + "같은 키의 성공/실패를 재생하고 동시 새 요청은 409입니다. 확정 AI 실패 발화는 저장하지 않습니다. 쇼핑은 비활성입니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 또는 저장 성공 재생"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 타인 대화방"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "중복 키 충돌, 처리 중, 시도 만료 또는 CONCURRENT_CHANGE 버전 경합"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "AI 호출 제한, Retry-After 제공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 응답 또는 인증 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 일시 불가, Retry-After 제공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "AI 시간 초과")})
    ResponseEntity<ApiResponse<ChatSessionReplyResponse>> send(@Parameter(hidden = true) Long userId, @Positive Long sessionId,
        @Valid SendChatMessageRequest request);
}
