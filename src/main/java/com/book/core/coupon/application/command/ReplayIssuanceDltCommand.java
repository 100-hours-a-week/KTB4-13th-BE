package com.book.core.coupon.application.command;

import java.util.List;

public record ReplayIssuanceDltCommand(List<Long> ids) {
    public ReplayIssuanceDltCommand {
        ids = List.copyOf(ids);
    }
}
