package com.book.core.review.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.common.response.PageResponse;
import com.book.core.review.api.converter.ReviewCommandConverter;
import com.book.core.review.api.request.CreateReviewRequest;
import com.book.core.review.api.request.UpdateReviewRequest;
import com.book.core.review.api.response.ReviewResponse;
import com.book.core.review.api.spec.ReviewControllerSpec;
import com.book.core.review.application.service.ReviewService;
import com.book.core.review.domain.enums.ReviewTargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Min;
import com.book.core.review.api.converter.ReviewResultConverter;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
class ReviewController implements ReviewControllerSpec {
    private final ReviewService reviewService;
    private final ReviewCommandConverter commandConverter;
    private final ReviewResultConverter resultConverter;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
        @RequestParam("targetType") final ReviewTargetType targetType, @Positive @RequestParam("targetId") final Long targetId,
        @Min(0) @RequestParam(value = "offset", defaultValue = "0") final int offset,
        @Positive @RequestParam(value = "limit", defaultValue = "20") final int limit) {
        final var command = commandConverter.toGetReviewsCommand(targetType, targetId, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(resultConverter.toResponse(reviewService.findReviews(command))));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReview(@UserId final Long userId,
        @Valid @RequestBody final CreateReviewRequest request) {
        reviewService.createReview(commandConverter.toCreateReviewCommand(userId, request));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> updateReview(@UserId final Long userId,
        @Positive @PathVariable("reviewId") final Long reviewId, @Valid @RequestBody final UpdateReviewRequest request) {
        reviewService.updateReview(commandConverter.toUpdateReviewCommand(userId, reviewId, request));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@UserId final Long userId,
        @Positive @PathVariable("reviewId") final Long reviewId) {
        reviewService.deleteReview(commandConverter.toDeleteReviewCommand(userId, reviewId));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
