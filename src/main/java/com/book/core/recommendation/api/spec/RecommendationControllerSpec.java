package com.book.core.recommendation.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.recommendation.api.request.ChatRecommendationRequest;
import com.book.core.recommendation.api.response.ChatRecommendationResponse;
import com.book.core.recommendation.api.response.RecommendationCardDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;

@Tag(name = "Recommendation", description = "대화형 도서 추천 API")
public interface RecommendationControllerSpec {
    @Operation(summary = "대화형 도서 추천",
        description = "현재 spec, message, recentTurns, excludeBookIds를 전달해 AI 추천 서비스를 호출하고 최대 3개의 추천 카드를 반환합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "AI 추천 서비스 요청 한도 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 추천 서비스 일시 불가"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "AI 추천 서비스 응답 지연")})
    ResponseEntity<ApiResponse<ChatRecommendationResponse>> chat(@Parameter(required = true, example = "1") @Positive final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChatRecommendationRequest.class)))
        @Valid final ChatRecommendationRequest request);

    @Operation(summary = "추천 카드 상세 조회", description = "저장된 recommendationCardId의 상세 정보를 조회합니다. AI를 다시 호출하지 않습니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 다른 사용자의 카드")})
    ResponseEntity<ApiResponse<RecommendationCardDetailResponse>> getCard(
        @Parameter(required = true, example = "1") @Positive final Long userId,
        @Parameter(required = true, example = "1") @Positive final Long recommendationCardId);
}
