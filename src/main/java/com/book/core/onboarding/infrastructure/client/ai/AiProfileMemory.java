package com.book.core.onboarding.infrastructure.client.ai;

import com.book.core.onboarding.application.port.AiPersonalizationProfileMemory;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.format.DateTimeFormatter;
import java.util.List;

record AiProfileMemory(@JsonProperty("type") String type, @JsonProperty("value") String value, @JsonProperty("strength") int strength,
    @JsonProperty("applies_to") List<String> appliesTo, @JsonProperty("keyword") String keyword,
    @JsonProperty("created_at") String createdAt, @JsonProperty("vector") List<Float> vector, @JsonProperty("dim") int dim) {
    private static final DateTimeFormatter CREATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    static AiProfileMemory from(final AiPersonalizationProfileMemory memory) {
        return new AiProfileMemory(memory.type(), memory.value(), memory.strength(), memory.appliesTo(), memory.keyword(),
            CREATED_AT.format(memory.createdAt()), memory.vector(), memory.dim());
    }
}
