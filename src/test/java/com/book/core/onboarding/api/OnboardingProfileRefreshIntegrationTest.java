package com.book.core.onboarding.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.port.AiPersonalizationProfileClient;
import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class OnboardingProfileRefreshIntegrationTest {
    private static final String MORNING = "아침, 하루를 시작할 때";
    private static final String LUNCH = "점심시간이나 짧은 휴식 시간";

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    AiPersonalizationProfileClient profileClient;

    private final List<AiPersonalizationProfileRequest> createRequests = new ArrayList<>();
    private final List<AiPersonalizationProfileRequest> updateRequests = new ArrayList<>();
    private final List<List<Long>> committedOptionIdsAtUpdate = new ArrayList<>();
    private final List<List<Long>> committedBookIdsAtUpdate = new ArrayList<>();

    @BeforeEach
    void recordProfileRequests() {
        doAnswer(invocation -> createRequests.add(invocation.getArgument(0))).when(profileClient).createProfile(any());
        doAnswer(invocation -> {
            final AiPersonalizationProfileRequest request = invocation.getArgument(0);
            updateRequests.add(request);
            committedOptionIdsAtUpdate.add(readOnNewConnection(() -> optionIds(request.userId())));
            committedBookIdsAtUpdate.add(readOnNewConnection(() -> bookIds(request.userId())));
            return null;
        }).when(profileClient).updateProfile(any());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 최초_온보딩은_생성을_요청하고_취향_정보가_변경되면_커밋된_데이터로_갱신을_요청하며_변경이_없으면_호출하지_않는다() throws Exception {
        final Long userId = insertUser("프로필갱신회원1");
        final Long firstBookId = insertBook("프로필 갱신 도서 1");
        final Long secondBookId = insertBook("프로필 갱신 도서 2");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(firstBookId);

        assertThat(createRequests).singleElement().satisfies(request -> assertThat(request.readingTimes()).containsExactly(MORNING));
        assertThat(updateRequests).isEmpty();

        updatePreferences(2, firstBookId).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        assertThat(updateRequests).hasSize(1);
        assertThat(updateRequests.getLast().readingTimes()).containsExactly(LUNCH);
        assertThat(committedOptionIdsAtUpdate.getLast()).contains(2L).doesNotContain(1L);

        updatePreferences(2, secondBookId).andExpect(status().isOk());
        assertThat(updateRequests).hasSize(2);
        assertThat(updateRequests.getLast().likedBookIds()).containsExactly(secondBookId);
        assertThat(committedBookIdsAtUpdate.getLast()).containsExactly(secondBookId);

        updatePreferences(2, secondBookId).andExpect(status().isOk());
        assertThat(updateRequests).hasSize(2);
        assertThat(createRequests).hasSize(1);
    }

    @Test
    void AI_프로필_갱신이_실패해도_수정_결과를_유지하고_200을_응답한다() throws Exception {
        final Long userId = insertUser("프로필갱신회원2");
        final Long bookId = insertBook("프로필 갱신 도서 3");
        authenticateAs(userId);
        agreePersonalization();
        completeOnboarding(bookId);
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).when(profileClient).updateProfile(any());

        updatePreferences(2, bookId).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        assertThat(optionIds(userId)).contains(2L).doesNotContain(1L);
        verify(profileClient).updateProfile(any());
    }

    @Test
    void 개인화_추천에_동의하지_않은_회원은_취향_정보가_변경되어도_AI를_호출하지_않는다() throws Exception {
        final Long userId = insertUser("프로필갱신회원3");
        final Long bookId = insertBook("프로필 갱신 도서 4");
        authenticateAs(userId);
        completeOnboarding(bookId);

        updatePreferences(2, bookId).andExpect(status().isOk());

        assertThat(optionIds(userId)).contains(2L);
        verify(profileClient, never()).createProfile(any());
        verify(profileClient, never()).updateProfile(any());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_term_agreement WHERE user_id = ?", Integer.class, userId)).isZero();
    }

    @Test
    void 취향_정보_수정이_실패하면_AI를_호출하지_않는다() throws Exception {
        final Long userId = insertUser("프로필갱신회원4");
        final Long bookId = insertBook("프로필 갱신 도서 5");
        authenticateAs(userId);
        agreePersonalization();
        saveAnswers(1, "[1]");

        updatePreferences(2, bookId).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("E409"));

        verify(profileClient, never()).createProfile(any());
        verify(profileClient, never()).updateProfile(any());
    }

    private void completeOnboarding(final Long bookId) throws Exception {
        saveAnswers(1, "[1]");
        saveAnswers(2, "[6]");
        saveAnswers(3, "[9]");
        saveAnswers(4, "[23]");
        mvc.perform(put("/api/v1/onboarding/books").contentType(MediaType.APPLICATION_JSON).content("{\"bookIds\":[" + bookId + "]}"))
            .andExpect(status().isOk());
    }

    private ResultActions updatePreferences(final long readingTimeOptionId, final Long bookId) throws Exception {
        return mvc.perform(put("/api/v1/onboarding/preferences").contentType(MediaType.APPLICATION_JSON).content("""
            {"answers":[{"questionId":1,"optionIds":[%d]},{"questionId":2,"optionIds":[6]},
            {"questionId":3,"optionIds":[9]},{"questionId":4,"optionIds":[23]}],"bookIds":[%d]}
            """.formatted(readingTimeOptionId, bookId)));
    }

    private void agreePersonalization() throws Exception {
        mvc.perform(post("/api/v1/onboarding/personalization-agreement")).andExpect(status().isOk());
    }

    private void saveAnswers(final int questionId, final String optionIds) throws Exception {
        mvc.perform(put("/api/v1/onboarding/questions/{questionId}/answers", questionId).contentType(MediaType.APPLICATION_JSON)
            .content("{\"optionIds\":" + optionIds + "}")).andExpect(status().isOk());
    }

    private static List<Long> readOnNewConnection(final Supplier<List<Long>> query) {
        return CompletableFuture.supplyAsync(query).join();
    }

    private List<Long> optionIds(final Long userId) {
        return jdbc.queryForList("SELECT onboarding_option_id FROM user_onboarding_answers WHERE user_id = ?", Long.class, userId);
    }

    private List<Long> bookIds(final Long userId) {
        return jdbc.queryForList("SELECT book_id FROM user_onboarding_books WHERE user_id = ?", Long.class, userId);
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private Long insertBook(final String title) {
        jdbc.update("INSERT INTO books (title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?)", title, "저자", "출판사",
            "카테고리", LocalDate.of(2026, 1, 1));
        return jdbc.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title);
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
