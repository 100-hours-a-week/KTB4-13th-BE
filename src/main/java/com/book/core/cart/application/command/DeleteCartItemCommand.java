package com.book.core.cart.application.command;

public record DeleteCartItemCommand(Long userId, Long cartItemId) {}
