package com.book.core.recommendation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.recommendation.api.converter.RecommendationCommandConverter;
import com.book.core.recommendation.api.converter.RecommendationResultConverter;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.port.RecommendationFeedItem;
import com.book.core.recommendation.application.port.RecommendationFeedResult;
import com.book.core.recommendation.application.result.ChatRecommendationResult;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import com.book.core.recommendation.application.result.RecommendationCardResult;
import com.book.core.recommendation.application.service.RecommendationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(RecommendationController.class)
@Import({RecommendationCommandConverter.class, RecommendationResultConverter.class,
    RecommendationControllerTest.AuthenticationPrincipalTestConfig.class})
@ActiveProfiles("test")
class RecommendationControllerTest {
    private static final String SPEC_JSON = """
        {
          "intent": "semantic",
          "exact": {},
          "filters": {},
          "semantic": "따뜻한 위로가 되는 소설",
          "anchor_book": null,
          "exclude": []
        }
        """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RecommendationService recommendationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 정상_요청이면_추천_결과를_반환하고_인증된_userId로_Command를_전달한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.chat(any())).thenReturn(new ChatRecommendationResult(Map.of("intent", "semantic"), "이 책들을 추천합니다",
            List.of(new RecommendationCardResult(1L, 10L, "제목", "작가", null, "긴 추천 이유")), "더 필요하신가요?", List.of("다시 추천"), false));

        mvc.perform(post("/api/v1/recommend/chat").contentType(MediaType.APPLICATION_JSON).content("""
            {
              "consented": true,
              "spec": %s,
              "message": "따뜻한 소설 추천해줘",
              "recentTurns": [],
              "excludeBookIds": [1, 2]
            }
            """.formatted(SPEC_JSON))).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reply").value("이 책들을 추천합니다")).andExpect(jsonPath("$.data.cards[0].recommendationCardId").value(1));

        final var expectedSpec = new java.util.HashMap<String, Object>();
        expectedSpec.put("intent", "semantic");
        expectedSpec.put("exact", Map.of());
        expectedSpec.put("filters", Map.of());
        expectedSpec.put("semantic", "따뜻한 위로가 되는 소설");
        expectedSpec.put("anchor_book", null);
        expectedSpec.put("exclude", List.of());

        verify(recommendationService)
            .chat(new ChatRecommendationCommand(42L, true, expectedSpec, "따뜻한 소설 추천해줘", List.of(), List.of(1L, 2L)));
    }

    @Test
    void spec에_필수_key가_없으면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/recommend/chat").contentType(MediaType.APPLICATION_JSON).content("""
            {
              "consented": true,
              "spec": {"intent": "semantic"},
              "message": "추천해줘",
              "recentTurns": []
            }
            """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void message가_200자를_초과하면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);
        final String tooLong = "가".repeat(201);

        mvc.perform(post("/api/v1/recommend/chat").contentType(MediaType.APPLICATION_JSON).content("""
            {
              "consented": true,
              "spec": %s,
              "message": "%s",
              "recentTurns": []
            }
            """.formatted(SPEC_JSON, tooLong))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void recentTurns가_20개를_초과하면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);
        final String turns = String.join(",", java.util.Collections.nCopies(21, "{\"role\": \"user\", \"text\": \"안녕\"}"));

        mvc.perform(post("/api/v1/recommend/chat").contentType(MediaType.APPLICATION_JSON).content("""
            {
              "consented": true,
              "spec": %s,
              "message": "추천해줘",
              "recentTurns": [%s]
            }
            """.formatted(SPEC_JSON, turns))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void 카드_상세_조회는_인증된_userId를_Command로_전달한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getCard(new GetRecommendationCardCommand(42L, 1L)))
            .thenReturn(new RecommendationCardDetailResult(1L, 10L, "제목", "작가", null, "긴 추천 이유", 5L, new BigDecimal("18000.00")));

        mvc.perform(get("/api/v1/recommend/cards/{recommendationCardId}", 1L)).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.recommendationCardId").value(1))
            .andExpect(jsonPath("$.data.productId").value(5));

        verify(recommendationService).getCard(new GetRecommendationCardCommand(42L, 1L));
    }

    @Test
    void 피드_조회는_인증된_userId로_Command를_전달하고_Cache_Control_헤더를_포함한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(new GetRecommendationFeedCommand(42L, 15, null))).thenReturn(new RecommendationFeedResult(
            List.of(new RecommendationFeedItem(3310L, "아무튼, 산", "장보영", new BigDecimal("9900"), "https://example.com/1.jpg", true, 84)),
            "next-page", false, null));

        mvc.perform(get("/api/v1/recommend/feed")).andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store")).andExpect(header().doesNotExist("X-Degraded"))
            .andExpect(jsonPath("$.data.items[0].bookId").value(3310)).andExpect(jsonPath("$.data.nextCursor").value("next-page"))
            .andExpect(jsonPath("$.data.coldStart").value(false));

        verify(recommendationService).getFeed(new GetRecommendationFeedCommand(42L, 15, null));
    }

    @Test
    void 요청에_userId_query가_있어도_인증된_userId만_사용한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(new GetRecommendationFeedCommand(42L, 15, null)))
            .thenReturn(new RecommendationFeedResult(List.of(), null, true, null));

        mvc.perform(get("/api/v1/recommend/feed").param("userId", "999")).andExpect(status().isOk());

        verify(recommendationService).getFeed(new GetRecommendationFeedCommand(42L, 15, null));
    }

    @Test
    void size와_cursor를_그대로_Command에_전달한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(new GetRecommendationFeedCommand(42L, 30, "abc")))
            .thenReturn(new RecommendationFeedResult(List.of(), null, false, null));

        mvc.perform(get("/api/v1/recommend/feed").param("size", "30").param("cursor", "abc")).andExpect(status().isOk());

        verify(recommendationService).getFeed(new GetRecommendationFeedCommand(42L, 30, "abc"));
    }

    @Test
    void surface가_home이_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void size가_50을_초과하면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(get("/api/v1/recommend/feed").param("size", "51")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void X_Degraded가_있으면_응답_헤더에_그대로_포함한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(new GetRecommendationFeedCommand(42L, 15, null)))
            .thenReturn(new RecommendationFeedResult(List.of(), null, false, "rule-only"));

        mvc.perform(get("/api/v1/recommend/feed")).andExpect(status().isOk()).andExpect(header().string("X-Degraded", "rule-only"));
    }

    @Test
    void 빈_목록도_200으로_응답한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(new GetRecommendationFeedCommand(42L, 15, null)))
            .thenReturn(new RecommendationFeedResult(List.of(), null, true, null));

        mvc.perform(get("/api/v1/recommend/feed")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
            .andExpect(jsonPath("$.data.coldStart").value(true));
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
