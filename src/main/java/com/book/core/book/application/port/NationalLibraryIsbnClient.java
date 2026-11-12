package com.book.core.book.application.port;

import java.time.LocalDate;
import java.util.List;

public interface NationalLibraryIsbnClient {
    Page findPaperBookIsbns(LocalDate from, LocalDate through, int pageNumber);

    record Page(List<String> candidateIsbns, boolean hasNext) {
    }
}
