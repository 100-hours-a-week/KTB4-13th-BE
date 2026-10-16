package com.book.core.cancel.application.command;

import com.book.core.cancel.domain.CancelAction;

public record CancelCommand(Long userId, CancelAction action) {
}
