package com.book.core.point.api.response;

import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// @formatter:off
public record PointHistoryResponse(Long id, Long userId, PointType type, Long referenceId, BigDecimal amount, LocalDateTime createdAt) {}
// @formatter:on
