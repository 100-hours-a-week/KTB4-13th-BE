package com.book.core.coupon.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.coupon.application.command.AcceptedCouponIssue;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.port.CouponIssueQueuePort;
import com.book.core.coupon.application.result.CouponIssueState;
import com.book.core.coupon.domain.CouponIssuePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;

@UseCase
@Profile({"coupon-streams", "coupon-kafka"})
@RequiredArgsConstructor
public class EnqueueCouponIssueUseCase {
    private final IssueCouponUseCase issueCouponUseCase;
    private final CouponIssueQueuePort queue;

    public void execute(final IssueCouponCommand command, final CouponIssueState issueState) {
        final CouponIssuePolicy policy = issueCouponUseCase.validateIssuable(command.userId(), command.couponId(), issueState);
        final AcceptedCouponIssue issue = new AcceptedCouponIssue(command.couponId(), command.userId(), policy.maxUseCount());
        switch (queue.reserve(issue)) {
            case ACCEPTED -> {
                return;
            }
            case DUPLICATE -> throw new CoreException(ErrorCode.COUPON_ALREADY_ISSUED);
            case SOLD_OUT -> throw new CoreException(ErrorCode.COUPON_SOLD_OUT);
            case UNAVAILABLE -> throw new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED);
            case FAILED -> throw new CoreException(ErrorCode.COUPON_ISSUE_FAILED);
        }
    }
}
