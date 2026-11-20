package com.book.core.product.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.product.application.usecase.RecordProductViewUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class ProductViewRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    RecordProductViewUseCase recordProductViewUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void 같은_상품을_두_번_조회하면_서로_다른_행으로_저장한다() {
        recordProductViewUseCase.execute(34001L, 82001L);
        recordProductViewUseCase.execute(34001L, 82001L);

        final List<Map<String, Object>> rows =
            jdbc.queryForList("SELECT id, user_id, product_id, viewed_at FROM product_views WHERE user_id = ? ORDER BY id", 34001L);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(row -> row.get("id")).doesNotHaveDuplicates();
        assertThat(rows).allSatisfy(row -> {
            assertThat(row).containsEntry("user_id", 34001L).containsEntry("product_id", 82001L);
            assertThat(row.get("viewed_at")).isNotNull();
        });
    }

    @Test
    void 상품_조회_트랜잭션이_롤백되어도_기록_트랜잭션은_별도로_커밋한다() {
        final var transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            recordProductViewUseCase.execute(34002L, 82002L);
            status.setRollbackOnly();
        });

        final Integer count =
            jdbc.queryForObject("SELECT COUNT(*) FROM product_views WHERE user_id = ? AND product_id = ?", Integer.class, 34002L, 82002L);
        assertThat(count).isEqualTo(1);
    }
}
