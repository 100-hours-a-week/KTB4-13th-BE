package com.book.core.book.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record DataForLibraryExternalBook(String isbn13, @JsonProperty("bookname") String title, @JsonProperty("authors") String author,
    String publisher, @JsonProperty("class_no") String category, @JsonProperty("publication_date") String publishedAt, String description,
    @JsonProperty("bookImageURL") String cover) {
}
