package com.book.core.recommendation.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class AiFeedEnvelope {
    private final AiFeedResponse data;

    @JsonCreator
    AiFeedEnvelope(@JsonProperty("data") final AiFeedResponse data) {
        this.data = data;
    }

    AiFeedResponse data() {
        return data;
    }
}
