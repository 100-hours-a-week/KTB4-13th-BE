package com.book.core.cancel.application.command;

import com.book.core.cancel.domain.CancelAction;

/** HTTP 식별자와 취소 액션을 전달합니다. */
public record CancelCommand(Long userId, CancelAction action) {
}
