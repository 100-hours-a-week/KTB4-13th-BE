package com.book.core.settlement.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.merchant.application.usecase.GetMerchantProductMappingsUseCase;
import com.book.core.merchant.application.usecase.GetMerchantsUseCase;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
class MerchantMappingIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private GetMerchantsUseCase merchants;
    @Autowired
    private GetMerchantProductMappingsUseCase mappings;
    @Autowired
    private JdbcTemplate jdbc;
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM merchant_product_mapping");
        jdbc.update("DELETE FROM merchant");
    }

    @Test
    void 가맹점조회와_상품매핑_조합고유제약을_확인한다() {
        merchant(73L, 333L);
        assertThat(merchants.execute(List.of(73L)).getFirst().settlementCycle()).isEqualTo(1);
        assertThat(mappings.execute(List.of(333L)).getFirst().merchantId()).isEqualTo(73L);
        assertThatThrownBy(() -> jdbc.update(
            "INSERT INTO merchant_product_mapping (merchant_id, product_id, created_at, updated_at) VALUES (73, 333, NOW(), NOW())"))
            .isInstanceOf(DataIntegrityViolationException.class);
        merchant(74L, 333L);
        assertThat(mappings.execute(List.of(333L))).hasSize(2);
    }

    private void merchant(final Long merchantId, final Long productId) {
        jdbc.update("INSERT INTO merchant (id, name, settlement_cycle, created_at, updated_at) VALUES (?, '테스트 가맹점', 1, NOW(), NOW())",
            merchantId);
        jdbc.update("INSERT INTO merchant_product_mapping (merchant_id, product_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
            merchantId, productId);
    }
}
