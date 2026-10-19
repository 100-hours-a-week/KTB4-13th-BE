package com.book.core.preference.api.response;

import java.time.LocalDate;
import java.util.List;

public record UserMemoryGroupResponse(List<Long> memoryIds, String type, String value, List<String> categories,
    List<LocalDate> conversationDates) {
}
