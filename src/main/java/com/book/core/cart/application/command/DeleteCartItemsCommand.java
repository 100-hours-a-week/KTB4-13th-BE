package com.book.core.cart.application.command;

import java.util.List;

public record DeleteCartItemsCommand(Long userId, List<Long> cartItemIds) {}
