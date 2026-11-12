package com.book.core.book.infrastructure.persistence.repository;

import com.book.core.book.application.result.NewBookIsbnPage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.port.DataForLibraryBookDetailClient;
import com.book.core.book.application.port.NationalLibraryIsbnClient;
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
    private static final BookDetail DETAIL = new BookDetail("신간", "저자", null, "출판사", "800", TODAY.minusDays(1), null);

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
    private NationalLibraryIsbnClient nationalLibrary;
    @MockitoBean
    private DataForLibraryBookDetailClient detail;

    @BeforeEach
    void resetCatalog() {
        jdbc.update("DELETE FROM books WHERE imported_isbn13 IN (?, ?)", ISBN_A, ISBN_B);
        jdbc.update("UPDATE book_detail_call_budget SET quota_day = NULL, calls = 0, next_call_at = NULL WHERE id = 1");
        when(nationalLibrary.findPaperBookIsbns(any(), any(), anyInt())).thenReturn(new NewBookIsbnPage(List.of(ISBN_A, ISBN_B), false));
    }

    @Test
    void 다른_세션은_상세_조회가_진행되는_동안_목록을_호출하지_않는다() throws Exception {
        final var started = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        when(detail.findBookDetail(ISBN_A)).thenAnswer((final var call) -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return Optional.of(DETAIL);
        });
        when(detail.findBookDetail(ISBN_B)).thenReturn(Optional.of(DETAIL));
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

        verify(nationalLibrary).findPaperBookIsbns(TODAY.minusDays(30), TODAY.minusDays(1), 1);
        verify(detail).findBookDetail(ISBN_A);
        verify(detail).findBookDetail(ISBN_B);
        assertThat(books.existsByIsbn13(ISBN_A)).isTrue();
        assertThat(books.existsByIsbn13(ISBN_B)).isTrue();
    }

    @Test
    void 다음_실행은_이미_저장된_도서를_건너뛰고_상세가_없었던_ISBN만_다시_조회한다() {
        when(detail.findBookDetail(ISBN_A)).thenReturn(Optional.empty(), Optional.of(DETAIL));
        when(detail.findBookDetail(ISBN_B)).thenReturn(Optional.of(DETAIL));

        imports.importNewBooks(TODAY);
        assertThat(books.existsByIsbn13(ISBN_A)).isFalse();
        assertThat(books.existsByIsbn13(ISBN_B)).isTrue();

        imports.importNewBooks(TODAY.plusDays(1));
        assertThat(books.existsByIsbn13(ISBN_A)).isTrue();
        verify(detail, times(2)).findBookDetail(ISBN_A);
        verify(detail).findBookDetail(ISBN_B);
    }

    @Test
    void 상세_요청이_두_건이면_같은_실행에서_연속_처리한다() {
        when(detail.findBookDetail(any())).thenReturn(Optional.of(DETAIL));

        imports.importNewBooks(TODAY);

        verify(detail).findBookDetail(ISBN_A);
        verify(detail).findBookDetail(ISBN_B);
    }
}
