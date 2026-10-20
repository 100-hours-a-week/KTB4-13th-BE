package com.book.core.point.application.result;

import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @formatter:off
public record PointHistoryResult(Long id, Long userId, PointType type, Long referenceId, BigDecimal amount, LocalDateTime createdAt) {}
// @formatter:on
