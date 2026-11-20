package com.book.core.library.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import com.book.common.config.security.UserIdArgumentResolver;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.library.api.converter.LibraryCommandConverter;
import com.book.core.library.application.port.ShelfPhotoStorageClient;
import com.book.core.library.application.service.LibraryService;
import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import com.book.core.library.application.usecase.CreateShelfPhotoUploadUseCase;
import com.book.core.library.application.usecase.DeleteLibraryBooksUseCase;
import com.book.core.library.application.usecase.GetLibraryBooksUseCase;
import com.book.core.library.application.usecase.ScanShelfPhotoUseCase;
import com.book.core.library.infrastructure.client.ai.ShelfScanTestClient;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** 실제 소켓으로 31초 늦게 도착한 AI 응답이 사진 인식 API에 전달되는지 확인합니다. */
@Tag("integration")
class ShelfScanSlowResponseIntegrationTest {
    private static final Duration AI_DELAY = Duration.ofSeconds(31);

    private HttpServer server;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void AI가_30초를_넘겨_응답해도_사진_전용_읽기_제한_안이면_결과를_받는다() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/shelf/scans", exchange -> {
            try {
                Thread.sleep(AI_DELAY);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            final byte[] body = """
                {"message":"shelf_scan_success","data":{"photo_type":"cover","items":[]}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream response = exchange.getResponseBody()) {
                response.write(body);
            }
        });
        server.start();
        final var client = ShelfScanTestClient.create("http://127.0.0.1:" + server.getAddress().getPort());
        final String photoKey = "shelf-photos/42/123e4567-e89b-12d3-a456-426614174000.jpg";
        final ShelfPhotoStorageClient storage = mock(ShelfPhotoStorageClient.class);
        given(storage.presignRead(photoKey)).willReturn("https://bucket.s3/" + photoKey);
        final LibraryService libraryService = new LibraryService(mock(GetLibraryBooksUseCase.class), mock(AddLibraryBooksUseCase.class),
            mock(GetBooksUseCase.class), mock(DeleteLibraryBooksUseCase.class), mock(CreateShelfPhotoUploadUseCase.class),
            new ScanShelfPhotoUseCase(storage, client), mock(GetAiBookIdentifiersUseCase.class));
        final var mvc = standaloneSetup(new LibraryController(libraryService, new LibraryCommandConverter()))
            .setCustomArgumentResolvers(new UserIdArgumentResolver()).build();
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        final long startedAt = System.nanoTime();

        mvc.perform(post("/api/v1/library/photos/scans").contentType(MediaType.APPLICATION_JSON)
            .content("{\"photoKey\":\"%s\"}".formatted(photoKey)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.photoType").value("cover")).andExpect(jsonPath("$.data.items").isEmpty());
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isGreaterThanOrEqualTo(AI_DELAY);
    }
}
