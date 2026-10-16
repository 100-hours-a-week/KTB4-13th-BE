package com.book.core.settlement.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.payment.domain.enums.TransactionType;
import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.application.port.SettlementTargetRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import com.book.core.settlement.domain.SettlementTarget;
import java.math.BigDecimal;
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
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class SettlementPersistenceIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private SettlementRepositoryPort settlements;
    @Autowired
    private SettlementTargetRepositoryPort targets;
    @Autowired
    private SettlementJpaRepository settlementJpaRepository;
    @Autowired
    private JdbcTemplate jdbc;
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @BeforeEach
    void clearData() {
        jdbc.update("DELETE FROM settlement_target");
        jdbc.update("DELETE FROM settlement");
        jdbc.update("DELETE FROM merchant_product_mapping");
        jdbc.update("DELETE FROM merchant");
    }

    @Test
    void 집계는_가맹점과_일자를_나누고_주문은_중복없이_센다() {
        targets.saveAll(List.of(target(1L, date, 10L, "300"), target(1L, date, 10L, "-100"), target(1L, date, 11L, "50"),
            target(2L, date, 10L, "80"), target(1L, date.minusDays(1), 10L, "999")));
        final var summaries = targets.findSummary(date);
        assertThat(summaries).hasSize(2);
        final var first = summaries.stream().filter((final var summary) -> summary.merchantId() == 1L).findFirst().orElseThrow();
        assertThat(first.targetAmount()).isEqualByComparingTo("250");
        assertThat(first.targetCount()).isEqualTo(3);
        assertThat(first.orderCount()).isEqualTo(2);
        assertThat(targets.findSummary(date.plusDays(1))).isEmpty();
    }

    @Test
    void 최근매출은_양끝날짜를_포함하고_SENT만_합산한다() {
        settlements.saveAll(List.of(settlement(1L, date.minusMonths(1), "10", SettlementState.SENT),
            settlement(1L, date.minusDays(1), "20", SettlementState.SENT), settlement(1L, date.minusDays(2), "999", SettlementState.READY),
            settlement(1L, date, "999", SettlementState.SENT),
            settlement(1L, date.minusMonths(1).minusDays(1), "999", SettlementState.SENT),
            settlement(2L, date.minusDays(1), "500", SettlementState.SENT)));
        final var result = settlements.readRecentSalesAmounts(date.minusMonths(1), date.minusDays(1), List.of(1L, 3L));
        assertThat(result.get(1L)).isEqualByComparingTo("30");
        assertThat(result).doesNotContainKeys(2L, 3L);
    }

    @Test
    void 정산생성의_가맹점_일자_고유제약과_기술예외를_유지한다() {
        settlements.saveAll(List.of(settlement(1L, date, "10", SettlementState.READY)));
        assertThatThrownBy(() -> settlementJpaRepository.saveAndFlush(settlement(1L, date, "20", SettlementState.READY)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(settlementJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 대상재적재는_참고와_동일하게_중복을_허용한다() {
        targets.saveAll(List.of(target(1L, date, 10L, "100")));
        targets.saveAll(List.of(target(1L, date, 10L, "100")));
        assertThat(targets.findSummary(date).getFirst().targetCount()).isEqualTo(2);
    }

    private Settlement settlement(final Long merchantId, final LocalDate day, final String originalAmount, final SettlementState state) {
        return new Settlement(merchantId, day, new BigDecimal(originalAmount), BigDecimal.ZERO, new BigDecimal("0.1"),
            new BigDecimal(originalAmount), state);
    }

    private SettlementTarget target(final Long merchantId, final LocalDate day, final Long orderId, final String amount) {
        return new SettlementTarget(merchantId, day, new BigDecimal(amount), TransactionType.PAYMENT, 1L, orderId, 1L, 1L,
            new BigDecimal(amount), new BigDecimal(amount));
    }
}
