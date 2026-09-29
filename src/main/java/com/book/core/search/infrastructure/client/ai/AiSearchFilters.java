package com.book.core.search.infrastructure.client.ai;

import com.book.core.search.application.port.BookSearchRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// @formatter:off
@JsonInclude(JsonInclude.Include.NON_NULL)
record AiSearchFilters(
    @JsonProperty("category") String category,
    @JsonProperty("price_min") Integer priceMin,
    @JsonProperty("price_max") Integer priceMax,
    @JsonProperty("pub_year_from") Integer pubYearFrom,
    @JsonProperty("pub_year_to") Integer pubYearTo
) {
    static AiSearchFilters from(final BookSearchRequest request) {
        return new AiSearchFilters(request.category(), request.priceMin(), request.priceMax(), request.pubYearFrom(), request.pubYearTo());
    }
}
// @formatter:on
