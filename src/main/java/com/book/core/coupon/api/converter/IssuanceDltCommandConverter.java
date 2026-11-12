package com.book.core.coupon.api.converter;

import com.book.core.coupon.api.request.ReplayIssuanceDltRequest;
import com.book.core.coupon.application.command.ReplayIssuanceDltCommand;
import org.springframework.stereotype.Component;

@Component
public class IssuanceDltCommandConverter {
    public ReplayIssuanceDltCommand toCommand(final ReplayIssuanceDltRequest request) {
        return new ReplayIssuanceDltCommand(request.ids());
    }
}
