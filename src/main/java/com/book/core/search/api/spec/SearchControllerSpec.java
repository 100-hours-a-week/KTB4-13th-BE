package com.book.core.search.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.search.api.response.BookSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;

@Tag(name = "Search", description = "도서 검색 API")
public interface SearchControllerSpec {
    @Operation(summary = "도서 검색",
        description = "검색어와 전달한 필터·정렬로 도서를 검색합니다. 검색어에서 조건을 추출하지 않습니다. "
            + "결과가 없으면 fallbackMessage에 AI 추천 안내 문구가 오고, 벡터 검색을 쓸 수 없으면 X-Degraded 헤더가 함께 옵니다. 항목의 productId는 활성 상품이 없는 도서면 null입니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식이나 범위가 올바르지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410",
            description = "cursor가 만료되었거나 검색 조건이 바뀌어 처음부터 다시 조회해야 함")})
    ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
        @Parameter(description = "검색어 1–200자", required = true, example = "여행의 이유") @NotBlank @Size(max = 200) final String query,
        @Parameter(description = "온보딩 관심 분류", example = "에세이") final String category,
        @Parameter(description = "판매가 하한(원, 포함)", example = "10000") @PositiveOrZero final Integer priceMin,
        @Parameter(description = "판매가 상한(원, 포함). priceMin보다 작으면 400", example = "20000") @PositiveOrZero final Integer priceMax,
        @Parameter(description = "출간연도 시작(포함)", example = "2020") @Positive final Integer pubYearFrom,
        @Parameter(description = "출간연도 종료(포함). pubYearFrom보다 작으면 400", example = "2024") @Positive final Integer pubYearTo,
        @Parameter(description = "popular(기본), newest, price_asc", example = "popular")
        @Pattern(regexp = "popular|newest|price_asc") final String sort, @Parameter(description = "직전 응답의 nextCursor") final String cursor,
        @Parameter(description = "한 페이지 개수. 기본 12, 최대 50", example = "12") @Positive @Max(50) final int size);
}
