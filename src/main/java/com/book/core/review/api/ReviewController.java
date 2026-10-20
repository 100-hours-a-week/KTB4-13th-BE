package com.book.core.review.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.common.response.PageResponse;
import com.book.core.review.api.converter.ReviewCommandConverter;
import com.book.core.review.api.request.AddReviewRequest;
import com.book.core.review.api.request.UpdateReviewRequest;
import com.book.core.review.api.response.ReviewResponse;
import com.book.core.review.api.spec.ReviewControllerSpec;
import com.book.common.paging.Page;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.service.ReviewService;
import com.book.core.review.domain.enums.ReviewTargetType;
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
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(@UserId(required = false) final Long userId,
        @RequestParam("targetType") final ReviewTargetType targetType, @RequestParam("targetId") final Long targetId,
        @RequestParam(value = "offset", defaultValue = "0") final int offset,
        @RequestParam(value = "limit", defaultValue = "20") final int limit) {
        final GetReviewsCommand command = commandConverter.toGetReviewsCommand(targetType, targetId, offset, limit);
        final Page<ReviewItemResult> result = reviewService.findReviews(command);
        final PageResponse<ReviewResponse> response = resultConverter.toResponse(result, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> addReview(@UserId final Long userId, @RequestBody final AddReviewRequest request) {
        final AddReviewCommand command = commandConverter.toAddReviewCommand(userId, request);
        final Long result = reviewService.addReview(command);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Override
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Long>> updateReview(@UserId final Long userId, @PathVariable("reviewId") final Long reviewId,
        @RequestBody final UpdateReviewRequest request) {
        final UpdateReviewCommand command = commandConverter.toUpdateReviewCommand(userId, reviewId, request);
        final Long result = reviewService.updateReview(command);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Long>> removeReview(@UserId final Long userId, @PathVariable("reviewId") final Long reviewId) {
        final RemoveReviewCommand command = commandConverter.toRemoveReviewCommand(userId, reviewId);
        final Long result = reviewService.removeReview(command);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
