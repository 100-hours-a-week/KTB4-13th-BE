package com.book.core.cart.application.command;

public record GetCartCommand(Long userId){public static GetCartCommand of(final Long userId){return new GetCartCommand(userId);}}
