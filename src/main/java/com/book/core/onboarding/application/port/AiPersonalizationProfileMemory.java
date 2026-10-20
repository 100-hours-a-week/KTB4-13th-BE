package com.book.core.onboarding.application.port;

import java.time.LocalDateTime;
import java.util.List;

public record AiPersonalizationProfileMemory(String type, String value, int strength, List<String> appliesTo, String keyword,
    LocalDateTime createdAt, List<Float> vector, int dim) {
}
