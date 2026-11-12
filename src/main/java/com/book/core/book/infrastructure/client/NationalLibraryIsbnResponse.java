package com.book.core.book.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record NationalLibraryIsbnResponse(@JsonProperty("TOTAL_COUNT") String totalCount, List<NationalLibraryIsbnDocument> docs) {
}
