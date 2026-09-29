package com.book.core.search.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// @formatter:off
@JsonIgnoreProperties(ignoreUnknown = true)
record AiSearchResponse(
    @JsonProperty("results") List<AiSearchItem> results,
    @JsonProperty("next_cursor") String nextCursor,
    @JsonProperty("fallback") AiSearchFallback fallback
) {}
// @formatter:on
