package com.book.core.coupon.application.command;

import java.util.List;

public record GetCouponsCommand(List<Long> couponIds) {
}
