package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.application.result.NewBookCandidate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.service.NewBookImportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class NewBookImportIntegrationTest {
    private static final String ISBN_A = "9788983921987";
    private static final String ISBN_B = "9788972756194";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
    private static final BookDetail DETAIL = new BookDetail("신간", "저자", null, "출판사", "800", null);
    private static final LocalDate PUBLISHED_AT_A = TODAY.minusDays(1);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8.0").withExposedPorts(6379);

    @Autowired
    private NewBookImportService imports;
    @Autowired
    private BookRepositoryPort books;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private TransactionTemplate transactions;

    @MockitoBean
    private NewBookCollector collector;

    @BeforeEach
    void resetCatalog() {
        jdbc.update("DELETE FROM books WHERE imported_isbn13 IN (?, ?)", ISBN_A, ISBN_B);
        jdbc.update("UPDATE book_detail_call_budget SET quota_day = NULL, calls = 0 WHERE id = 1");
        when(collector.collectCandidates(any(), any())).thenReturn(
            List.of(new NewBookCandidate(ISBN_A, PUBLISHED_AT_A), new NewBookCandidate(ISBN_B, TODAY.minusDays(2))));
    }

    @Test
    void 상세가_없었던_ISBN은_다음_실행에서_다시_조회한다() {
        when(collector.findDetail(ISBN_A)).thenReturn(Optional.empty(), Optional.of(DETAIL));
        when(collector.findDetail(ISBN_B)).thenReturn(Optional.of(DETAIL));

        imports.importNewBooks(TODAY);
        assertThat(books.existsByIsbn13(ISBN_A)).isFalse();
        assertThat(books.existsByIsbn13(ISBN_B)).isTrue();

        imports.importNewBooks(TODAY.plusDays(1));
        assertThat(books.existsByIsbn13(ISBN_A)).isTrue();
        verify(collector, times(2)).findDetail(ISBN_A);
        verify(collector).findDetail(ISBN_B);
    }

    @Test
    void 상세_요청이_두_건이면_같은_실행에서_연속_처리한다() {
        when(collector.findDetail(any())).thenReturn(Optional.of(DETAIL));

        imports.importNewBooks(TODAY);

        verify(collector).findDetail(ISBN_A);
        verify(collector).findDetail(ISBN_B);
        assertThat(jdbc.queryForObject("SELECT published_at FROM books WHERE imported_isbn13 = ?", LocalDate.class, ISBN_A))
            .isEqualTo(PUBLISHED_AT_A);
    }

}
