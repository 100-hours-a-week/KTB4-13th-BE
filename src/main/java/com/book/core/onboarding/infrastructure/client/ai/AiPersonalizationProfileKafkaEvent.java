package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
record AiPersonalizationProfileKafkaEvent(@JsonProperty("schema_version") int schemaVersion, @JsonProperty("operation") String operation,
    @JsonProperty("user_id") Long userId, @JsonProperty("job_revision") UUID jobRevision,
    @JsonProperty("profile") AiProfileRequest profile) {
}
