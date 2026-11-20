package com.book.core.book.application.port;

import com.book.core.book.application.result.NewBookIsbnPage;
import java.time.LocalDate;

public interface NewBookIsbnFinder {
    NewBookIsbnPage findPaperBookIsbns(LocalDate from, LocalDate through, int pageNumber);
}
