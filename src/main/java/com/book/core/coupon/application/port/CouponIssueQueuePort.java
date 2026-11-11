package com.book.core.coupon.application.port;

import com.book.core.coupon.application.command.AcceptedCouponIssue;

public interface CouponIssueQueuePort {
    enum Reservation {
        ACCEPTED,
        SOLD_OUT,
        DUPLICATE,
        UNAVAILABLE,
        FAILED
    }

    Reservation reserve(AcceptedCouponIssue issue);
}
