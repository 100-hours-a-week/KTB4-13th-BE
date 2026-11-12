package com.book.core.book.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record DataForLibraryDetailEntry(DataForLibraryExternalBook book) {
}
