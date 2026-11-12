package com.book.core.book.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
record DataForLibraryDetailResponse(List<DataForLibraryDetailEntry> detail, JsonNode error) {
}
