package com.book.core.book.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record NationalLibraryIsbnDocument(@JsonProperty("EA_ISBN") String isbn13) {
}
