package com.book.core.search.api;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.product.application.usecase.GetRecommendationProductsUseCase;
import com.book.core.search.api.converter.SearchCommandConverter;
import com.book.core.search.application.service.SearchService;
import com.book.core.search.application.usecase.SearchBooksUseCase;
import com.book.core.search.infrastructure.client.ai.BookSearchClientImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@Transactional
class SearchProductConsistencyIntegrationTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private GetAiBookIdentifiersUseCase identifiers;

    @Autowired
    private GetRecommendationProductsUseCase products;

    @Test
    void 검색_응답의_가격은_AI_값을_유지하고_재고는_BE_상품을_기준으로_반환한다() throws Exception {
        insertProduct(1523701L, "9788900237001", 0);
        insertProduct(1523702L, "9788900237018", 3);

        final var builder = RestClient.builder().baseUrl("http://ai.test");
        final var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ai.test/search")).andRespond(withSuccess("""
            {
              "message": "search_success",
              "data": {
                "results": [
                  {"isbn": "9788900237001", "title": "품절 도서", "price": 13500, "in_stock": true},
                  {"isbn": "9788900237018", "title": "판매 중인 도서", "price": 16000, "in_stock": false}
                ],
                "next_cursor": "next-page"
              }
            }
            """, MediaType.APPLICATION_JSON));
        final var client = new BookSearchClientImpl(builder.build(), identifiers);
        final var service = new SearchService(new SearchBooksUseCase(client, products));
        final var mvc = standaloneSetup(new SearchController(service, new SearchCommandConverter())).build();

        mvc.perform(get("/api/v1/search").param("query", "도서")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].productId").value(1523701))
            .andExpect(jsonPath("$.data.items[0].price").value(13500))
            .andExpect(jsonPath("$.data.items[0].inStock").value(false))
            .andExpect(jsonPath("$.data.items[1].productId").value(1523702))
            .andExpect(jsonPath("$.data.items[1].price").value(16000))
            .andExpect(jsonPath("$.data.items[1].inStock").value(true))
            .andExpect(jsonPath("$.data.nextCursor").value("next-page"));
        server.verify();
    }

    private void insertProduct(final long id, final String isbn, final int stockQuantity) {
        jdbc.update("""
            INSERT INTO books (id, isbn13, title, author, publisher, category, published_at, created_at, updated_at)
            VALUES (?, ?, '도서', '저자', '출판사', '소설', '2026-01-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, id, isbn);
        jdbc.update("""
            INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity, created_at, updated_at)
            VALUES (?, ?, '상품', 20000, 18000, 12000, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, id, id);
        jdbc.update("INSERT INTO product_inventory (product_id, on_hand_quantity) VALUES (?, ?)", id, stockQuantity);
    }
}
