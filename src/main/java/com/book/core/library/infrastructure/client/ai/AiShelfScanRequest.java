package com.book.core.library.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

record AiShelfScanRequest(@JsonProperty("user_id") Long userId, @JsonProperty("image_ref") String imageRef) {
}
