package com.book.core.coupon.application.port;

public interface IssuanceRequestPublisher {
    void publishAndWait(String messageKey, String payload);
}
