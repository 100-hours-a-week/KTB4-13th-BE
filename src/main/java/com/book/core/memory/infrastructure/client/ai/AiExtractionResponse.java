package com.book.core.memory.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AiExtractionResponse(@JsonProperty("extractions") List<AiExtraction> extractions,
    @JsonProperty("nothing_found") Boolean nothingFound) {
}
