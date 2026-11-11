package com.book.core.library.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.library.api.converter.LibraryCommandConverter;
import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.command.CreateShelfPhotoUploadCommand;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.command.ScanShelfPhotoCommand;
import com.book.core.library.application.result.LibraryBookResult;
import com.book.core.library.application.result.ShelfPhotoUploadResult;
import com.book.core.library.application.result.ShelfScanResult.Spine;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.application.service.LibraryService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LibraryController.class)
@Import({LibraryCommandConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class LibraryControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    LibraryService libraryService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 목록_조회는_JWT_회원_ID로_조회하고_상품이_없는_항목은_productId를_null로_응답한다() throws Exception {
        authenticateAs(42L);
        given(libraryService.getBooks(42L)).willReturn(
            List.of(new LibraryBookResult(11L, 101L, 201L, "제목", "저자", "https://image.test/cover.jpg", LocalDateTime.of(2026, 10, 2, 9, 0)),
                new LibraryBookResult(12L, 102L, null, "상품 없는 책", "저자2", null, LocalDateTime.of(2026, 10, 1, 9, 0))));

        mvc.perform(get("/api/v1/library/books").param("userId", "99")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.books.length()").value(2))
            .andExpect(jsonPath("$.data.books[0].libraryBookId").value(11)).andExpect(jsonPath("$.data.books[0].bookId").value(101))
            .andExpect(jsonPath("$.data.books[0].productId").value(201)).andExpect(jsonPath("$.data.books[0].title").value("제목"))
            .andExpect(jsonPath("$.data.books[0].author").value("저자"))
            .andExpect(jsonPath("$.data.books[0].coverImageUrl").value("https://image.test/cover.jpg"))
            .andExpect(jsonPath("$.data.books[0].addedAt").value("2026-10-02T09:00:00"))
            .andExpect(jsonPath("$.data.books[1].productId").value((Object) null))
            .andExpect(jsonPath("$.data.books[1].coverImageUrl").value((Object) null));
    }

    @Test
    void 담기는_JWT_회원_ID와_고른_도서_ID로_Command를_전달한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(
            post("/api/v1/library/books").param("userId", "99").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[101,102]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        verify(libraryService).addBooks(new AddLibraryBooksCommand(42L, List.of(101L, 102L)));
    }

    @Test
    void 존재하지_않는_도서를_담으면_404를_응답한다() throws Exception {
        authenticateAs(42L);
        willThrow(new CoreException(ErrorCode.LIBRARY_BOOK_NOT_FOUND)).given(libraryService)
            .addBooks(new AddLibraryBooksCommand(42L, List.of(999L)));

        mvc.perform(post("/api/v1/library/books").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[999]}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"));
    }

    @ParameterizedTest
    @MethodSource("invalidAddBooksBodies")
    void 담을_도서_목록이_없거나_잘못되거나_50권을_넘으면_400을_응답하고_Service를_호출하지_않는다(final String body) throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/library/books").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(libraryService);
    }

    private static Stream<String> invalidAddBooksBodies() {
        final String fiftyOneBookIds = IntStream.rangeClosed(1, 51).mapToObj(String::valueOf).collect(Collectors.joining(","));
        return Stream.of("{}", "{\"bookIds\":[]}", "{\"bookIds\":[1,0]}", "{\"bookIds\":[1,null]}",
            "{\"bookIds\":[" + fiftyOneBookIds + "]}");
    }

    @Test
    void 삭제는_JWT_회원_ID와_요청_ID로_Command를_전달한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(delete("/api/v1/library/books").param("userId", "99").contentType(MediaType.APPLICATION_JSON)
            .content("{\"libraryBookIds\":[11,12,11]}")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        verify(libraryService).deleteBooks(new DeleteLibraryBooksCommand(42L, List.of(11L, 12L, 11L)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"libraryBookIds\":[]}", "{\"libraryBookIds\":[11,0]}", "{\"libraryBookIds\":[11,null]}"})
    void 삭제_목록이_없거나_잘못된_ID가_있으면_400을_응답하고_Service를_호출하지_않는다(final String body) throws Exception {
        authenticateAs(42L);

        mvc.perform(delete("/api/v1/library/books").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(libraryService);
    }

    @Test
    void 업로드_주소_발급은_JWT_회원_ID로_요청하고_서명_헤더와_만료_시각을_응답한다() throws Exception {
        authenticateAs(42L);
        given(libraryService.createPhotoUpload(new CreateShelfPhotoUploadCommand(42L, "image/jpeg", 2048L)))
            .willReturn(new ShelfPhotoUploadResult("shelf-photos/42/a.jpg", "https://s3/upload", Map.of("content-type", "image/jpeg"),
                Instant.parse("2026-11-10T01:05:00Z")));

        mvc.perform(post("/api/v1/library/photos/upload-url").param("userId", "99").contentType(MediaType.APPLICATION_JSON)
            .content("{\"contentType\":\"image/jpeg\",\"contentLength\":2048}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.photoKey").value("shelf-photos/42/a.jpg"))
            .andExpect(jsonPath("$.data.uploadUrl").value("https://s3/upload"))
            .andExpect(jsonPath("$.data.headers.content-type").value("image/jpeg"))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-11-10T01:05:00Z"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"contentType\":\"image/jpeg\"}", "{\"contentType\":\"\",\"contentLength\":1}",
        "{\"contentType\":\"image/jpeg\",\"contentLength\":0}"})
    void 업로드_주소_요청_형식이_잘못되면_400을_응답한다(final String body) throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/library/photos/upload-url").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(libraryService);
    }

    @Test
    void 사진_인식은_JWT_회원_ID와_사진_키로_요청하고_찾은_책과_못_찾은_책을_응답한다() throws Exception {
        authenticateAs(42L);
        given(libraryService.scanPhoto(new ScanShelfPhotoCommand(42L, "shelf-photos/42/a.jpg"))).willReturn(new ShelfScanResult("shelf",
            List.of(
                new ShelfScanResult.Item("FOUND", "루비", new ShelfScanResult.Book(101L, "9788970507538", "루비", "저자", "출판사", 2012, null),
                    List.of(new Spine(5, List.of(List.of(0.27, 0.24))))),
                new ShelfScanResult.Item("NOT_FOUND", "COVERT JAVA", null, List.of()))));

        mvc.perform(post("/api/v1/library/photos/scans").param("userId", "99").contentType(MediaType.APPLICATION_JSON)
            .content("{\"photoKey\":\"shelf-photos/42/a.jpg\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.photoType").value("shelf")).andExpect(jsonPath("$.data.items[0].state").value("FOUND"))
            .andExpect(jsonPath("$.data.items[0].book.bookId").value(101))
            .andExpect(jsonPath("$.data.items[0].book.isbn").value("9788970507538"))
            .andExpect(jsonPath("$.data.items[0].spines[0].order").value(5))
            .andExpect(jsonPath("$.data.items[0].spines[0].polygon[0][0]").value(0.27))
            .andExpect(jsonPath("$.data.items[1].state").value("NOT_FOUND"))
            .andExpect(jsonPath("$.data.items[1].readText").value("COVERT JAVA"))
            .andExpect(jsonPath("$.data.items[1].book").value((Object) null));
    }

    @Test
    void 다른_회원의_사진을_인식하면_403을_응답한다() throws Exception {
        authenticateAs(7L);
        given(libraryService.scanPhoto(new ScanShelfPhotoCommand(7L, "shelf-photos/42/a.jpg")))
            .willThrow(new CoreException(ErrorCode.SHELF_PHOTO_FORBIDDEN));

        mvc.perform(post("/api/v1/library/photos/scans").contentType(MediaType.APPLICATION_JSON)
            .content("{\"photoKey\":\"shelf-photos/42/a.jpg\"}")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void 사진_키가_비었으면_400을_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/library/photos/scans").contentType(MediaType.APPLICATION_JSON).content("{\"photoKey\":\" \"}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(libraryService);
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
