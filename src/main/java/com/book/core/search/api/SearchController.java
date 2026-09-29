package com.book.core.search.api;

import com.book.common.response.ApiResponse;
import com.book.core.search.api.converter.SearchCommandConverter;
import com.book.core.search.api.converter.SearchResultConverter;
import com.book.core.search.api.response.BookSearchResponse;
import com.book.core.search.api.spec.SearchControllerSpec;
import com.book.core.search.application.port.BookSearchResult;
import com.book.core.search.application.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/search")
class SearchController implements SearchControllerSpec {
    private final SearchService searchService;
    private final SearchCommandConverter commandConverter;
    private final SearchResultConverter resultConverter;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(@NotBlank @Size(max = 200) @RequestParam("query") final String query,
        @RequestParam(value = "category", required = false) final String category,
        @PositiveOrZero @RequestParam(value = "priceMin", required = false) final Integer priceMin,
        @PositiveOrZero @RequestParam(value = "priceMax", required = false) final Integer priceMax,
        @Positive @RequestParam(value = "pubYearFrom", required = false) final Integer pubYearFrom,
        @Positive @RequestParam(value = "pubYearTo", required = false) final Integer pubYearTo,
        @Pattern(regexp = "popular|newest|price_asc")
        @RequestParam(value = "sort", required = false, defaultValue = "popular") final String sort,
        @RequestParam(value = "cursor", required = false) final String cursor,
        @Positive @Max(50) @RequestParam(value = "size", required = false, defaultValue = "12") final int size) {
        final var command =
            commandConverter.toSearchBooksCommand(query, category, priceMin, priceMax, pubYearFrom, pubYearTo, sort, cursor, size);
        final BookSearchResult result = searchService.searchBooks(command);
        final var response = resultConverter.toBookSearchResponse(result);

        final ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        if (result.degraded() != null) {
            responseBuilder.header("X-Degraded", result.degraded());
        }
        return responseBuilder.body(ApiResponse.ok(response));
    }
}
