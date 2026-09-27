package com.book.core.recommendation.api;

import com.book.common.response.ApiResponse;
import com.book.core.recommendation.api.converter.RecommendationCommandConverter;
import com.book.core.recommendation.api.converter.RecommendationResultConverter;
import com.book.core.recommendation.api.request.ChatRecommendationRequest;
import com.book.core.recommendation.api.response.ChatRecommendationResponse;
import com.book.core.recommendation.api.response.RecommendationCardDetailResponse;
import com.book.core.recommendation.api.spec.RecommendationControllerSpec;
import com.book.core.recommendation.application.service.RecommendationService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<ChatRecommendationResponse>> chat(@Positive @RequestParam("userId") final Long userId,
        @Valid @RequestBody final ChatRecommendationRequest request) {
        final var command = commandConverter.toChatRecommendationCommand(userId, request);
        final var result = recommendationService.chat(command);
        final var response = resultConverter.toChatRecommendationResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @GetMapping("/cards/{recommendationCardId}")
    public ResponseEntity<ApiResponse<RecommendationCardDetailResponse>> getCard(@Positive @RequestParam("userId") final Long userId,
        @Positive @PathVariable("recommendationCardId") final Long recommendationCardId) {
        final var command = commandConverter.toGetRecommendationCardCommand(userId, recommendationCardId);
        final var result = recommendationService.getCard(command);
        final var response = resultConverter.toRecommendationCardDetailResponse(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
