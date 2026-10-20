package com.book.core.agentchat.infrastructure.persistence.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.product.application.usecase.GetProductsForCartChangeUseCase;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.port.AiRecommendationCard;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import com.book.core.recommendation.application.usecase.ChatRecommendationUseCase;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class ShoppingQueryCountIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManagerFactory factory;
    @Autowired
    PlatformTransactionManager transactions;
    @Autowired
    ChatRecommendationUseCase recommendations;
    @Autowired
    GetCartUseCase carts;
    @Autowired
    GetProductsForCartChangeUseCase products;
    @MockitoBean
    AiRecommendationClient ai;

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void 추천_보강은_카드_수와_무관하게_두_번_조회한다(final int count) {
        final List<Long> ids = insertProducts(10000 + count * 100, count);
        when(ai.chat(any())).thenReturn(new AiRecommendationChatResult(Map.of(), "reply",
            ids.stream().map((final Long id) -> new AiRecommendationCard(id, 90, "이유", "긴 이유")).toList(), null, List.of(), false));
        final Statistics statistics = resetStatistics();
        try {
            assertThat(recommendations.execute(new ChatRecommendationCommand(1L, true, Map.of(), "추천", List.of(), List.of())).cards())
                .hasSize(count);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 30})
    void 장바구니_조회는_항목_수와_무관하게_세_번_조회한다(final int count) {
        final long userId = 20000 + count * 100;
        final List<Long> ids = insertProducts((int) userId, count);
        jdbc.update("INSERT INTO carts (id, user_id) VALUES (?, ?)", userId, userId);
        for (final Long id : ids) {
            jdbc.update("INSERT INTO cart_item (cart_id, product_id, quantity) VALUES (?, ?, 1)", userId, id);
        }
        final Statistics statistics = resetStatistics();
        try {
            assertThat(carts.execute(userId).items()).hasSize(count);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 30})
    void 상품_검증은_일괄조회_한번과_상품별_도서별_버전갱신을_구분한다(final int count) {
        final List<Long> ids = insertProducts(30000 + count * 100, count);
        final Statistics statistics = resetStatistics();
        try {
            new TransactionTemplate(transactions)
                .executeWithoutResult((final TransactionStatus status) -> assertThat(products.execute(ids)).hasSize(count));
            assertThat(statistics.getQueryExecutionCount()).isEqualTo(1);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1 + 2L * count);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    private Statistics resetStatistics() {
        final Statistics statistics = factory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        return statistics;
    }

    private List<Long> insertProducts(final int base, final int count) {
        final List<Long> ids = IntStream.range(0, count).mapToObj((final int index) -> (long) base + index).toList();
        for (final Long id : ids) {
            jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) "
                + "VALUES (?, '책', '작가', '출판사', '소설', '2026-01-01')", id);
            jdbc.update("INSERT INTO products (id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity) "
                + "VALUES (?, ?, '상품', 20000, 18000, 10000, 50)", id, id);
            jdbc.update("INSERT INTO product_inventory (product_id, on_hand_quantity) VALUES (?, ?)", id, 50);
        }
        return ids;
    }
}
