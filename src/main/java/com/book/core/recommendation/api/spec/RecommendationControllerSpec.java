package com.book.core.recommendation.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.recommendation.api.request.ChatRecommendationRequest;
import com.book.core.recommendation.api.response.ChatRecommendationResponse;
import com.book.core.recommendation.api.response.RecommendationCardDetailResponse;
import com.book.core.recommendation.api.response.RecommendationFeedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Tag(name = "Recommendation", description = "대화형 도서 추천 API")
public interface RecommendationControllerSpec {
    @Operation(summary = "대화형 도서 추천",
        description = "인증된 사용자 기준으로 현재 spec, message, recentTurns, excludeBookIds를 전달해 AI 추천 서비스를 호출하고 "
            + "최대 3개의 추천 카드를 반환합니다. 카드의 productId와 price는 활성 상품이 없는 도서면 null입니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "AI 추천 서비스 요청 한도 초과"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 추천 서비스 일시 불가"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "AI 추천 서비스 응답 지연")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<ChatRecommendationResponse>> chat(@Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ChatRecommendationRequest.class)))
        @Valid final ChatRecommendationRequest request);

    @Operation(summary = "추천 카드 상세 조회", description = "인증된 사용자의 저장 카드에서 recommendationCardId, bookId, reasonLong을 조회합니다. AI를 다시 호출하지 않습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 다른 사용자의 카드")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<RecommendationCardDetailResponse>> getCard(@Parameter(hidden = true) final Long userId,
        @Parameter(required = true, example = "1") @Positive final Long recommendationCardId);

    @Operation(summary = "추천 피드 조회",
        description = "인증된 회원의 취향 프로필을 기반으로 추천 목록을 조회합니다. surface=home은 정렬·필터를 받지 않고, "
            + "surface=recommend_more만 sort(match 기본, newest, price_asc)와 category, 출간연도 구간, matchScoreMin 필터를 받습니다. "
            + "AI가 축소 응답을 반환하면 X-Degraded 헤더가 함께 옵니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않거나 home에 정렬·필터를 보냄"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "cursor가 만료되어 처음부터 다시 조회해야 함")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<RecommendationFeedResponse>> getFeed(@Parameter(hidden = true) final Long userId,
        @Parameter(description = "home 또는 recommend_more", example = "home") @Pattern(regexp = "home|recommend_more") final String surface,
        @Parameter(example = "15") @Positive @Max(50) final int size, @Parameter(required = false) final String cursor,
        @Parameter(description = "recommend_more 전용. match(기본), newest, price_asc", example = "match")
        @Pattern(regexp = "match|newest|price_asc") final String sort,
        @Parameter(description = "recommend_more 전용. 온보딩 관심 분류", example = "에세이") final String category,
        @Parameter(description = "recommend_more 전용. 출간연도 시작(포함)", example = "2020") @Positive final Integer pubYearFrom,
        @Parameter(description = "recommend_more 전용. 출간연도 종료(포함). pubYearFrom보다 작으면 400", example = "2024")
        @Positive final Integer pubYearTo,
        @Parameter(description = "recommend_more 전용. 매칭 점수 하한 0–100", example = "70") @Min(0) @Max(100) final Integer matchScoreMin);
}
