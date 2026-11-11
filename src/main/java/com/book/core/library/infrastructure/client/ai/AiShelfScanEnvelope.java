package com.book.core.library.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record AiShelfScanEnvelope(@JsonProperty("message") String message, @JsonProperty("data") Data data) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Data(@JsonProperty("photo_type") String photoType, @JsonProperty("items") List<Item> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(@JsonProperty("state") String state, @JsonProperty("read_text") String readText, @JsonProperty("book") Book book,
        @JsonProperty("spines") List<Spine> spines) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Book(@JsonProperty("isbn") String isbn, @JsonProperty("title") String title, @JsonProperty("author") String author,
        @JsonProperty("publisher") String publisher, @JsonProperty("year") Integer year, @JsonProperty("cover_url") String coverUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Spine(@JsonProperty("order") Integer order, @JsonProperty("polygon") List<List<Double>> polygon) {
    }
}
