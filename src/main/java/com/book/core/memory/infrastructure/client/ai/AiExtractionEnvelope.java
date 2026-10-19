package com.book.core.memory.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record AiExtractionEnvelope(String message, AiExtractionResponse data) {
}
