package com.book.core.review.application.service;

import com.book.core.review.application.ReviewHistoryRecorder;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewFormat;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.enums.ReviewEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewWriteService {
    private final AddReviewUseCase addReviewUseCase;
    private final UpdateReviewUseCase updateReviewUseCase;
    private final DeleteReviewUseCase deleteReviewUseCase;
    private final ValidateReviewPolicyUseCase validateReviewPolicyUseCase;
    private final ReviewHistoryRecorder historyRecorder;

    @Transactional
    public ReviewProcessResult addReview(final AddReviewCommand command) {
        final ReviewKey reviewKey = validateReviewPolicyUseCase.validateNew(command.userId(), command.target());
        final Review review = addReviewUseCase.execute(command.target(), reviewKey, command.content());
        historyRecorder.record(review, ReviewEventType.CREATE);
        return new ReviewProcessResult(review.id(), ReviewFormat.TEXT);
    }

    @Transactional
    public Long updateReview(final UpdateReviewCommand command) {
        validateReviewPolicyUseCase.validateUpdate(command.userId(), command.reviewId());
        updateReviewUseCase.execute(command.userId(), command.reviewId(), command.content())
            .ifPresent((final var review) -> historyRecorder.record(review, ReviewEventType.UPDATE));
        return command.reviewId();
    }

    @Transactional
    public ReviewProcessResult removeReview(final RemoveReviewCommand command) {
        final Review review = deleteReviewUseCase.execute(command.userId(), command.reviewId());
        historyRecorder.record(review, ReviewEventType.DELETE);
        return new ReviewProcessResult(review.id(), ReviewFormat.TEXT);
    }
}
