package com.book.core.cart.application.command;

public record ModifyCartItemCommand(Long userId, Long cartItemId, Integer quantity) {}
