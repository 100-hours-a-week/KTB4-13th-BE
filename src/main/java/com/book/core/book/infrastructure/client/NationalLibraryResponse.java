package com.book.core.book.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record NationalLibraryResponse(
    @JsonProperty("RESULT") String result,
    @JsonProperty("ERR_CODE") String errorCode,
    @JsonProperty("TOTAL_COUNT") String totalCount,
    List<Book> docs) {
    record Book(
        @JsonProperty("EA_ISBN") String isbn13,
        @JsonProperty("REAL_PUBLISH_DATE") String realPublishDate,
        @JsonProperty("PUBLISH_PREDATE") String publishPredate) {
    }
}
