package com.book.core.agentchat.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.agentchat.api.request.ConfirmCartProposalRequest;
import com.book.core.agentchat.api.response.AgentCartExecutionResponse;
import com.book.core.agentchat.api.response.AgentCartProposalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;

@Tag(name = "쇼핑 에이전트", description = "BE가 저장한 변경 제안 승인과 과거 성공 결과 조회")
@SecurityRequirement(name = "bearerAuth")
public interface AgentCartControllerSpec {
    @Operation(summary = "제안 확인 후 실행 생성", description = "상품·수량·사용자는 변경할 수 없습니다. 첫 성공 201, 성공 재생 200. 거절은 400/404/409이며 전부 미적용입니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "성공 실행 생성, Location 제공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공 재생"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "기준 변경·만료·중복 키·버전 경합"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "없거나 타인 제안")})
    ResponseEntity<ApiResponse<AgentCartExecutionResponse>> confirm(Long userId, @Valid ConfirmCartProposalRequest request);

    @Operation(summary = "변경 제안 조회", description = "실행 가능 기간이 끝나도 제안 기록은 보존됩니다. 소유자만 조회합니다.")
    ResponseEntity<ApiResponse<AgentCartProposalResponse>> proposal(Long userId, @Positive Long proposalId);

    @Operation(summary = "과거 성공 실행 조회", description = "현재 장바구니와 구분되는 불변 성공 결과입니다. 소유자만 조회합니다.")
    ResponseEntity<ApiResponse<AgentCartExecutionResponse>> execution(Long userId, @Positive Long executionId);
}
