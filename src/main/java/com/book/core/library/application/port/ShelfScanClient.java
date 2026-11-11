package com.book.core.library.application.port;

import com.book.core.library.application.result.ShelfScanResult;

public interface ShelfScanClient {
    /** AI 카탈로그의 도서 ID는 BE 도서 ID와 다를 수 있어 받지 않으므로, 반환한 결과의 bookId는 비어 있습니다. */
    ShelfScanResult scan(Long userId, String imageRef);
}
