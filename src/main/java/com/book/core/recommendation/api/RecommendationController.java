package com.book.core.recommendation.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.recommendation.api.converter.RecommendationCommandConverter;
import com.book.core.recommendation.api.converter.RecommendationResultConverter;
import com.book.core.recommendation.api.request.ChatRecommendationRequest;
import com.book.core.recommendation.api.response.ChatRecommendationResponse;
import com.book.core.recommendation.api.response.RecommendationCardDetailResponse;
import com.book.core.recommendation.api.response.RecommendationFeedResponse;
import com.book.core.recommendation.api.spec.RecommendationControllerSpec;
import com.book.core.recommendation.application.result.GetRecommendationFeedResult;
import com.book.core.recommendation.application.service.RecommendationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/recommend")
class RecommendationController implements RecommendationControllerSpec {
    private final RecommendationService recommendationService;
    private final RecommendationCommandConverter commandConverter;
    private final RecommendationResultConverter resultConverter;

    @Override
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatRecommendationResponse>> chat(@UserId final Long userId,
        @Valid @RequestBody final ChatRecommendationRequest request) {
        final var command = commandConverter.toChatRecommendationCommand(userId, request);
        final var result = recommendationService.chat(command);
        final var response = resultConverter.toChatRecommendationResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @GetMapping("/cards/{recommendationCardId}")
    public ResponseEntity<ApiResponse<RecommendationCardDetailResponse>> getCard(@UserId final Long userId,
        @Positive @PathVariable("recommendationCardId") final Long recommendationCardId) {
        final var command = commandConverter.toGetRecommendationCardCommand(userId, recommendationCardId);
        final var result = recommendationService.getCard(command);
        final var response = resultConverter.toRecommendationCardDetailResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<RecommendationFeedResponse>> getFeed(@UserId final Long userId,
        @Pattern(regexp = "home|recommend_more")
        @RequestParam(value = "surface", required = false, defaultValue = "home") final String surface,
        @Positive @Max(50) @RequestParam(value = "size", required = false, defaultValue = "15") final int size,
        @RequestParam(value = "cursor", required = false) final String cursor,
        @Pattern(regexp = "match|newest|price_asc") @RequestParam(value = "sort", required = false) final String sort,
        @RequestParam(value = "category", required = false) final String category,
        @Positive @RequestParam(value = "pubYearFrom", required = false) final Integer pubYearFrom,
        @Positive @RequestParam(value = "pubYearTo", required = false) final Integer pubYearTo,
        @Min(0) @Max(100) @RequestParam(value = "matchScoreMin", required = false) final Integer matchScoreMin) {
        final var command = commandConverter.toGetRecommendationFeedCommand(userId, surface, size, cursor, sort, category, pubYearFrom,
            pubYearTo, matchScoreMin);
        final GetRecommendationFeedResult result = recommendationService.getFeed(command);
        final var response = resultConverter.toRecommendationFeedResponse(result);

        final ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        if (result.degraded() != null) {
            responseBuilder.header("X-Degraded", result.degraded());
        }
        return responseBuilder.body(ApiResponse.ok(response));
    }
}
