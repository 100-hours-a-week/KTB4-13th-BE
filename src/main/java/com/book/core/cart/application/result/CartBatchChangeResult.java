package com.book.core.cart.application.result;

import com.book.common.exception.ErrorCode;
import java.time.Instant;

/** 변경 적용·업무 거절·기한 경과를 반환합니다. */
public record CartBatchChangeResult(Instant appliedAt, ErrorCode rejection, boolean expired) {
}
