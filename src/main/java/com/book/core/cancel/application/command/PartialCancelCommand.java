package com.book.core.cancel.application.command;

import com.book.core.cancel.domain.PartialCancelAction;

public record PartialCancelCommand(Long userId, PartialCancelAction action) {
}
