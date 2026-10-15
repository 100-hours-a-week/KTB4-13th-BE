package com.book.core.cancel.application.command;

import com.book.core.cancel.domain.PartialCancelAction;

/** HTTP 식별자와 취소 액션을 전달합니다. */
public record PartialCancelCommand(Long userId, PartialCancelAction action) {
}
