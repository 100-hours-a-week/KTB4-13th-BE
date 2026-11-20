package com.book.core.book.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record JeongboNaruResponse(Body response) {
    record Body(String errCode, String error, List<Detail> detail) {
    }

    record Detail(Book book) {
    }

    record Book(
        String isbn13,
        String bookname,
        String authors,
        String publisher,
        @JsonProperty("class_no") String classNo,
        String description,
        @JsonProperty("bookImageURL") String bookImageUrl) {
    }
}
