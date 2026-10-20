package com.book.core.onboarding.application.port;

import java.time.LocalDateTime;
import java.util.List;

// createdAt은 AI 명세에 맞춰 한국 시간 기준입니다.
public record AiPersonalizationProfileMemory(String type, String value, int strength, List<String> appliesTo, String keyword,
    LocalDateTime createdAt, List<Float> vector, int dim) {
}
