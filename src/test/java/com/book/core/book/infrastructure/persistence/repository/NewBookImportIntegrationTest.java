package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.application.result.NewBookCandidate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.port.NewBookLookupBackoff;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.service.NewBookImportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private NewBookLookupBackoff backoff;

    @MockitoBean
    private NewBookCollector collector;

    @BeforeEach
    void resetCatalog() {
        jdbc.update("DELETE FROM books WHERE imported_isbn13 IN (?, ?)", ISBN_A, ISBN_B);
        jdbc.update("UPDATE book_detail_call_budget SET quota_day = NULL, calls = 0, next_call_at = NULL WHERE id = 1");
        jdbc.update("DELETE FROM new_book_lookup_backoff WHERE isbn13 IN (?, ?)", ISBN_A, ISBN_B);
        when(collector.collectCandidates(any(), any())).thenReturn(
            List.of(new NewBookCandidate(ISBN_A, PUBLISHED_AT_A), new NewBookCandidate(ISBN_B, TODAY.minusDays(2))));
    }

    @Test
    void 다른_세션은_상세_조회가_진행되는_동안_목록을_호출하지_않는다() throws Exception {
        final var started = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        when(collector.collectDetail(ISBN_A)).thenAnswer((final var call) -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return Optional.of(DETAIL);
        });
        when(collector.collectDetail(ISBN_B)).thenReturn(Optional.of(DETAIL));
        try (var executor = Executors.newSingleThreadExecutor()) {
            final var first = executor.submit(() -> imports.importNewBooks(TODAY));
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                imports.importNewBooks(TODAY);
            } finally {
                release.countDown();
            }
            first.get(5, TimeUnit.SECONDS);
        }

        verify(collector).collectCandidates(TODAY.minusDays(30), TODAY.minusDays(1));
        verify(collector).collectDetail(ISBN_A);
        verify(collector).collectDetail(ISBN_B);
        assertThat(books.existsByIsbn13(ISBN_A)).isTrue();
        assertThat(books.existsByIsbn13(ISBN_B)).isTrue();
    }

    @Test
    void 상세가_없었던_ISBN은_7일_동안_호출_한도를_쓰지_않고_건너뛴다() {
        when(collector.collectDetail(ISBN_A)).thenReturn(Optional.empty(), Optional.of(DETAIL));
        when(collector.collectDetail(ISBN_B)).thenReturn(Optional.of(DETAIL));

        imports.importNewBooks(TODAY);
        assertThat(books.existsByIsbn13(ISBN_A)).isFalse();
        assertThat(books.existsByIsbn13(ISBN_B)).isTrue();

        imports.importNewBooks(TODAY.plusDays(6));
        verify(collector).collectDetail(ISBN_A);
        assertThat(books.existsByIsbn13(ISBN_A)).isFalse();

        imports.importNewBooks(TODAY.plusDays(7));
        assertThat(books.existsByIsbn13(ISBN_A)).isTrue();
        verify(collector, times(2)).collectDetail(ISBN_A);
        verify(collector).collectDetail(ISBN_B);
    }

    @Test
    void 일시_오류가_반복되면_재시도_간격을_1일_2일_4일로_늘린다() {
        backoff.deferAfterFailure(ISBN_A, TODAY);
        assertThat(retryOn()).isEqualTo(TODAY.plusDays(1));
        backoff.deferAfterFailure(ISBN_A, TODAY);
        assertThat(retryOn()).isEqualTo(TODAY.plusDays(2));
        backoff.deferAfterFailure(ISBN_A, TODAY);
        assertThat(retryOn()).isEqualTo(TODAY.plusDays(4));
        backoff.deferAfterFailure(ISBN_A, TODAY);
        assertThat(retryOn()).isEqualTo(TODAY.plusDays(4));
        assertThat(backoff.isWaiting(ISBN_A, TODAY.plusDays(3))).isTrue();
        assertThat(backoff.isWaiting(ISBN_A, TODAY.plusDays(4))).isFalse();
    }

    @Test
    void 상세_요청이_두_건이면_같은_실행에서_연속_처리한다() {
        when(collector.collectDetail(any())).thenReturn(Optional.of(DETAIL));

        imports.importNewBooks(TODAY);

        verify(collector).collectDetail(ISBN_A);
        verify(collector).collectDetail(ISBN_B);
        assertThat(jdbc.queryForObject("SELECT published_at FROM books WHERE imported_isbn13 = ?", LocalDate.class, ISBN_A))
            .isEqualTo(PUBLISHED_AT_A);
    }

    private LocalDate retryOn() {
        return jdbc.queryForObject("SELECT retry_on FROM new_book_lookup_backoff WHERE isbn13 = ?", LocalDate.class, ISBN_A);
    }
}
