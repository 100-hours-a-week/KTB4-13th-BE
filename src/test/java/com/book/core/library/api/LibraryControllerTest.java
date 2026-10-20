package com.book.core.library.api;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.library.api.converter.LibraryCommandConverter;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.result.LibraryBookResult;
import com.book.core.library.application.service.LibraryService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
