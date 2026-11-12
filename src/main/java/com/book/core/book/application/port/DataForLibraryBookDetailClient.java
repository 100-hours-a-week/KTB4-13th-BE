package com.book.core.book.application.port;

import com.book.core.book.application.result.BookDetail;
import java.util.Optional;

public interface DataForLibraryBookDetailClient {
    Optional<BookDetail> findBookDetail(String isbn13);
}
