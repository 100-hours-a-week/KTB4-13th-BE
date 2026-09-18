package com.book.core.cart.application.command;

public record AddCartItemCommand(
    Long userId,
    Long productId,
    Integer quantity
) {}
