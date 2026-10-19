package com.book.core.library.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.library.api.converter.LibraryCommandConverter;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import com.book.core.library.application.service.LibraryService;
import java.time.Instant;
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
