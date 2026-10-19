package com.book.core.memory.application.result;

import com.book.core.memory.domain.UserMemoryType;
import java.time.LocalDate;
import java.util.List;

public record UserMemoryGroupResult(List<Long> memoryIds, UserMemoryType type, String value, List<String> categories,
    List<LocalDate> conversationDates) {
}
