package com.book.core.agentchat.application.result;

import java.math.BigDecimal;

public record AgentCheckout(Long useOwnedCouponId, BigDecimal usePoint) {
}
