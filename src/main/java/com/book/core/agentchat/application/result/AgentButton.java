package com.book.core.agentchat.application.result;

public record AgentButton(String label, String action, String target, Long proposalId) {
    public static final String CHECKOUT_TARGET = "ORDER-001";

    public boolean opensCheckout() {
        return "navigate".equals(action) && CHECKOUT_TARGET.equals(target);
    }
}
