package com.book.core.book.application.port;

import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookCandidate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NewBookCollector {
    List<NewBookCandidate> collectCandidates(LocalDate from, LocalDate through);

    Optional<BookDetail> collectDetail(String isbn13);
}
