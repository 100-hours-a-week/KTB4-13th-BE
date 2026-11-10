package com.book.core.onboarding.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

record AiPersonalizationProfileKafkaEvent(@JsonProperty("schema_version") int schemaVersion, @JsonProperty("job_revision") UUID jobRevision,
    @JsonProperty("profile") AiProfileRequest profile) {
}
