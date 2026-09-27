package com.book.core.review.api;

import com.book.common.response.ApiResponse;
import com.book.core.review.api.converter.ReviewCommandConverter;
import com.book.core.review.api.request.CreateReviewRequest;
import com.book.core.review.api.request.UpdateReviewRequest;
import com.book.core.review.api.spec.ReviewControllerSpec;
import com.book.core.review.application.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviews")
class ReviewController implements ReviewControllerSpec {
    private final ReviewService reviewService;
    private final ReviewCommandConverter commandConverter;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createReview(@Valid @RequestBody final CreateReviewRequest request) {
        reviewService.createReview(commandConverter.toCreateReviewCommand(request));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> updateReview(@Positive @PathVariable("reviewId") final Long reviewId,
        @Valid @RequestBody final UpdateReviewRequest request) {
        reviewService.updateReview(commandConverter.toUpdateReviewCommand(reviewId, request));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@Positive @PathVariable("reviewId") final Long reviewId) {
        reviewService.deleteReview(commandConverter.toDeleteReviewCommand(reviewId));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
