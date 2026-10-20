package com.book.core.library.api.response;

import com.book.core.library.application.result.LibraryBookResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

// @formatter:off
public record LibraryBookResponse(
        @Schema(description = "도서관 항목 ID. 삭제 API의 libraryBookIds에 사용합니다.") Long libraryBookId,
        @Schema(description = "도서 ID") Long bookId,
        @Schema(description = "판매 중인 상품 ID. 연결된 활성 상품이 없으면 null", nullable = true) Long productId,
        @Schema(description = "도서 제목") String title,
        @Schema(description = "저자") String author,
        @Schema(description = "표지 이미지 URL", nullable = true) String coverImageUrl,
        @Schema(description = "도서관에 추가한 시각") LocalDateTime addedAt) {

    public static LibraryBookResponse from(final LibraryBookResult result) {
        return new LibraryBookResponse(result.libraryBookId(), result.bookId(), result.productId(), result.title(), result.author(),
            result.coverImageUrl(), result.addedAt());
    }
}
// @formatter:on
