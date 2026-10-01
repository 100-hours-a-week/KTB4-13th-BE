package com.book.core.search.api;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.api.config.SwaggerConfig;
import com.book.core.search.api.converter.SearchCommandConverter;
import com.book.core.search.application.command.BookSearchSort;
import com.book.core.search.application.command.SearchBooksCommand;
import com.book.core.search.application.result.SearchBookItemResult;
import com.book.core.search.application.result.SearchBooksResult;
import com.book.core.search.application.service.SearchService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
@Import({SearchCommandConverter.class, SwaggerConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class})
@ActiveProfiles("test")
class SearchControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    SearchService searchService;

    @Test
    void 검색어만_보내면_popular_정렬과_크기_12로_검색하고_결과를_응답한다() throws Exception {
        when(searchService.searchBooks(any())).thenReturn(new SearchBooksResult(
            List.of(new SearchBookItemResult(2077L, 501L, "여행의 이유", "김영하", "문학동네", new BigDecimal("13500"), true,
                "https://example.com/2077.jpg"), new SearchBookItemResult(4400L, null, "상품 없는 책", "작가", "출판사", null, false, null)),
            "next-page", null, null));

        mvc.perform(get("/api/v1/search").param("query", "여행")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].bookId").value(2077)).andExpect(jsonPath("$.data.items[0].productId").value(501))
            .andExpect(jsonPath("$.data.items[1].bookId").value(4400)).andExpect(jsonPath("$.data.items[1].productId").value(nullValue()))
            .andExpect(jsonPath("$.data.items[0].title").value("여행의 이유")).andExpect(jsonPath("$.data.items[0].author").value("김영하"))
            .andExpect(jsonPath("$.data.items[0].publisher").value("문학동네")).andExpect(jsonPath("$.data.items[0].price").value(13500))
            .andExpect(jsonPath("$.data.items[0].inStock").value(true))
            .andExpect(jsonPath("$.data.items[0].coverUrl").value("https://example.com/2077.jpg"))
            .andExpect(jsonPath("$.data.nextCursor").value("next-page")).andExpect(jsonPath("$.data.fallbackMessage").doesNotExist())
            .andExpect(jsonPath("$.data.totalCount").doesNotExist()).andExpect(header().doesNotExist("X-Degraded"));

        verify(searchService).searchBooks(new SearchBooksCommand("여행", null, null, null, null, null, BookSearchSort.POPULAR, null, 12));
    }

    @Test
    void 필터와_정렬과_cursor와_크기를_Command로_전달한다() throws Exception {
        when(searchService.searchBooks(any())).thenReturn(emptyResult());

        mvc.perform(
            get("/api/v1/search").param("query", "투자 입문").param("category", "경제경영").param("priceMin", "10000").param("priceMax", "20000")
                .param("pubYearFrom", "2020").param("pubYearTo", "2024").param("sort", "newest").param("cursor", "abc").param("size", "20"))
            .andExpect(status().isOk());

        verify(searchService)
            .searchBooks(new SearchBooksCommand("투자 입문", "경제경영", 10000, 20000, 2020, 2024, BookSearchSort.NEWEST, "abc", 20));
    }

    @Test
    void popular와_price_asc_정렬을_받는다() throws Exception {
        when(searchService.searchBooks(any())).thenReturn(emptyResult());

        mvc.perform(get("/api/v1/search").param("query", "소설").param("sort", "popular")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/search").param("query", "소설").param("sort", "price_asc")).andExpect(status().isOk());

        verify(searchService).searchBooks(new SearchBooksCommand("소설", null, null, null, null, null, BookSearchSort.POPULAR, null, 12));
        verify(searchService).searchBooks(new SearchBooksCommand("소설", null, null, null, null, null, BookSearchSort.PRICE_ASC, null, 12));
    }

    @Test
    void 결과가_없으면_AI_fallback_문구와_X_Degraded를_그대로_전달한다() throws Exception {
        when(searchService.searchBooks(any()))
            .thenReturn(new SearchBooksResult(List.of(), null, "원하는 책을 못 찾았어요. AI 추천에게 물어볼까요?", "keyword-only"));

        mvc.perform(get("/api/v1/search").param("query", "없는 책")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
            .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
            .andExpect(jsonPath("$.data.fallbackMessage").value("원하는 책을 못 찾았어요. AI 추천에게 물어볼까요?"))
            .andExpect(header().string("X-Degraded", "keyword-only"));
    }

    @Test
    void 검색어가_없거나_비었거나_200자를_넘으면_400을_응답한다() throws Exception {
        mvc.perform(get("/api/v1/search")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(get("/api/v1/search").param("query", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search").param("query", "가".repeat(201))).andExpect(status().isBadRequest());

        verifyNoInteractions(searchService);
    }

    @Test
    void 가격이나_출간연도_범위가_뒤집히면_400을_응답한다() throws Exception {
        mvc.perform(get("/api/v1/search").param("query", "소설").param("priceMin", "20000").param("priceMax", "10000"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(get("/api/v1/search").param("query", "소설").param("pubYearFrom", "2024").param("pubYearTo", "2020"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(searchService);
    }

    @Test
    void 지원하지_않는_정렬이나_범위_밖_크기나_음수_가격은_400을_응답한다() throws Exception {
        mvc.perform(get("/api/v1/search").param("query", "소설").param("sort", "relevance")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search").param("query", "소설").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search").param("query", "소설").param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search").param("query", "소설").param("priceMin", "-1")).andExpect(status().isBadRequest());

        verifyNoInteractions(searchService);
    }

    private static SearchBooksResult emptyResult() {
        return new SearchBooksResult(List.of(), null, null, null);
    }

    @Test
    void 검색_명세는_비회원_공개_API로_OpenAPI에_노출된다() throws Exception {
        mvc.perform(get("/v3/api-docs/general")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/search'].get.summary").value("도서 검색"))
            .andExpect(jsonPath("$.paths['/api/v1/search'].get.security").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/v1/search'].get.responses['401']").doesNotExist());
    }
}
