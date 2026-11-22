package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import io.micrometer.observation.ObservationRegistry;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.client.RestClientException;

class NewBookCollectorClientTest {
    private static final String ISBN = "9788983921987";
    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);
    private final AtomicReference<String> query = new AtomicReference<>();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void 국중도_목록_조건을_인코딩해_보내고_응답을_record로_읽는다() {
        respond("/seoji/SearchApi.do", 200, """
            {"PAGE_NO":"1","TOTAL_COUNT":"3","docs":[
             {"EA_ISBN":"9788983921987","TITLE":"신간","REAL_PUBLISH_DATE":"","PUBLISH_PREDATE":"20261111"}]}
            """);

        final var response = nationalLibrary("test+key").searchPaperBooks(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 11, 11), 1, 100);

        assertThat(response.totalCount()).isEqualTo("3");
        assertThat(response.docs()).containsExactly(new NationalLibraryResponse.Book(ISBN, "", "20261111"));
        assertThat(query.get()).contains("cert_key=test%2Bkey", "start_publish_date=20261013", "end_publish_date=20261111", "ebook_yn=N",
            "page_no=1", "page_size=100", "form=%EC%A2%85%EC%9D%B4%EC%B1%85");
    }

    @Test
    void 정보나루_상세_조건을_인코딩해_보내고_응답을_record로_읽는다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"detail":[{"book":{"isbn13":"9788983921987","bookname":"신간","authors":"저자","publisher":"출판사",
             "class_no":"813.7","bookImageURL":"https://image"}}]}}
            """);

        final var response = jeongboNaru("test+key").findBookDetail(ISBN);

        assertThat(response.response().detail()).extracting(JeongboNaruResponse.Detail::book)
            .containsExactly(new JeongboNaruResponse.Book(ISBN, "신간", "저자", "출판사", "813.7", null, "https://image"));
        assertThat(query.get()).contains("authKey=test%2Bkey", "loaninfoYN=N", "format=json", "isbn13=" + ISBN);
    }

    @ParameterizedTest
    @CsvSource({"429, RATE_LIMIT", "500, TEMPORARY", "503, TEMPORARY", "400, REJECTED", "403, REJECTED"})
    void HTTP_상태를_실패_사유로_분류하고_응답이나_키를_예외에_담지_않는다(final int status, final NewBookCollectorFailureReason reason) {
        respond("/api/srchDtlList", status, "external response");
        final var client = jeongboNaru("sensitive-key");

        assertThatThrownBy(() -> client.findBookDetail(ISBN)).isInstanceOfSatisfying(NewBookCollectorException.class, (final var error) -> {
            assertThat(error.reason()).isEqualTo(reason);
            assertThat(error.getMessage()).isEqualTo(reason.name());
            assertThat(error.externalCode()).isNull();
            assertThat(error.getCause()).isNull();
        });
    }

    @Test
    void 읽기_시간을_넘기면_통신_오류로_끝난다() {
        server.createContext("/api/srchDtlList", (final var exchange) -> {
            try (exchange) {
                Thread.sleep(READ_TIMEOUT.multipliedBy(3).toMillis());
                exchange.sendResponseHeaders(200, -1);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        final var client = jeongboNaru("key");

        assertThatThrownBy(() -> client.findBookDetail(ISBN)).isInstanceOf(RestClientException.class);
    }

    @Test
    void 응답_형식이_깨지면_통신_오류로_끝난다() {
        respond("/api/srchDtlList", 200, """
            {"response":{"detail":"not-array"}}
            """);
        final var client = jeongboNaru("key");

        assertThatThrownBy(() -> client.findBookDetail(ISBN)).isInstanceOf(RestClientException.class);
    }

    private NationalLibraryClient nationalLibrary(final String key) {
        return NewBookCollectorClientConfiguration.createClient(NationalLibraryClient.class, baseUrl, "nationalLibraryKey", key,
            READ_TIMEOUT, ObservationRegistry.NOOP);
    }

    private JeongboNaruClient jeongboNaru(final String key) {
        return NewBookCollectorClientConfiguration.createClient(JeongboNaruClient.class, baseUrl, "jeongboNaruKey", key, READ_TIMEOUT,
            ObservationRegistry.NOOP);
    }

    private void respond(final String path, final int status, final String body) {
        server.createContext(path, (final var exchange) -> {
            try (exchange) {
                query.set(exchange.getRequestURI().getRawQuery());
                final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
    }
}
