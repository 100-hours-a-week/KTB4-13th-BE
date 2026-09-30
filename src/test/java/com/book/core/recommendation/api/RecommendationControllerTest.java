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

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.api.converter.RecommendationCommandConverter;
import com.book.core.recommendation.api.converter.RecommendationResultConverter;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.command.GetRecommendationFeedCommand;
import com.book.core.recommendation.application.command.RecommendationFeedSort;
import com.book.core.recommendation.application.command.RecommendationFeedSurface;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationController.class)
@Import({RecommendationCommandConverter.class, RecommendationResultConverter.class, UserIdMvcConfig.class})
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

        mvc.perform(post("/api/v1/recommend/chat").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
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
            .chat(new ChatRecommendationCommand(42L, 42L, true, expectedSpec, "따뜻한 소설 추천해줘", List.of(), List.of(1L, 2L)));
    }

    @Test
    void userId_query가_JWT의_ID와_다르면_403을_응답한다() throws Exception {
        authenticateAs(42L);
        final var expectedSpec = new java.util.HashMap<String, Object>();
        expectedSpec.put("intent", "semantic");
        expectedSpec.put("exact", Map.of());
        expectedSpec.put("filters", Map.of());
        expectedSpec.put("semantic", "따뜻한 위로가 되는 소설");
        expectedSpec.put("anchor_book", null);
        expectedSpec.put("exclude", List.of());
        final var command = new ChatRecommendationCommand(42L, 43L, true, expectedSpec, "추천해줘", List.of(), List.of(1L, 2L));
        when(recommendationService.chat(command)).thenThrow(new CoreException(ErrorCode.FORBIDDEN));

        mvc.perform(post("/api/v1/recommend/chat").param("userId", "43").contentType(MediaType.APPLICATION_JSON).content("""
            {
              "consented": true,
              "spec": %s,
              "message": "추천해줘",
              "recentTurns": [],
              "excludeBookIds": [1, 2]
            }
            """.formatted(SPEC_JSON))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E403"));

        verify(recommendationService).chat(command);
    }

    @Test
    void spec에_필수_key가_없으면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/recommend/chat").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
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

        mvc.perform(post("/api/v1/recommend/chat").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
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

        mvc.perform(post("/api/v1/recommend/chat").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
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
    void userId_query가_없으면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/recommend/chat").contentType(MediaType.APPLICATION_JSON).content("""
            {
              "consented": true,
              "spec": %s,
              "message": "추천해줘",
              "recentTurns": []
            }
            """.formatted(SPEC_JSON))).andExpect(status().isBadRequest());

        verifyNoInteractions(recommendationService);
    }

    @Test
    void 카드_상세_조회는_인증된_userId를_Command로_전달한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getCard(new GetRecommendationCardCommand(42L, 1L)))
            .thenReturn(new RecommendationCardDetailResult(1L, 10L, "긴 추천 이유"));

        mvc.perform(get("/api/v1/recommend/cards/{recommendationCardId}", 1L)).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.recommendationCardId").value(1))
            .andExpect(jsonPath("$.data.bookId").value(10)).andExpect(jsonPath("$.data.reasonLong").value("긴 추천 이유"))
            .andExpect(jsonPath("$.data.title").doesNotExist()).andExpect(jsonPath("$.data.author").doesNotExist())
            .andExpect(jsonPath("$.data.coverImageUrl").doesNotExist()).andExpect(jsonPath("$.data.productId").doesNotExist())
            .andExpect(jsonPath("$.data.price").doesNotExist());

        verify(recommendationService).getCard(new GetRecommendationCardCommand(42L, 1L));
    }

    @Test
    void 피드_조회는_인증된_userId로_Command를_전달하고_Cache_Control을_직접_설정하지_않는다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(homeCommand(15, null))).thenReturn(new RecommendationFeedResult(
            List.of(new RecommendationFeedItem(3310L, "아무튼, 산", "장보영", new BigDecimal("9900"), "https://example.com/1.jpg", true, 84)),
            "next-page", false, null));

        mvc.perform(get("/api/v1/recommend/feed")).andExpect(status().isOk()).andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
            .andExpect(header().doesNotExist("X-Degraded")).andExpect(jsonPath("$.data.items[0].bookId").value(3310))
            .andExpect(jsonPath("$.data.items[0].title").value("아무튼, 산")).andExpect(jsonPath("$.data.items[0].author").value("장보영"))
            .andExpect(jsonPath("$.data.items[0].price").value(9900))
            .andExpect(jsonPath("$.data.items[0].coverUrl").value("https://example.com/1.jpg"))
            .andExpect(jsonPath("$.data.items[0].inStock").value(true)).andExpect(jsonPath("$.data.items[0].matchScore").value(84))
            .andExpect(jsonPath("$.data.nextCursor").value("next-page")).andExpect(jsonPath("$.data.coldStart").value(false));

        verify(recommendationService).getFeed(homeCommand(15, null));
    }

    @Test
    void 요청에_userId_query가_있어도_인증된_userId만_사용한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(homeCommand(15, null))).thenReturn(new RecommendationFeedResult(List.of(), null, true, null));

        mvc.perform(get("/api/v1/recommend/feed").param("userId", "999")).andExpect(status().isOk());

        verify(recommendationService).getFeed(homeCommand(15, null));
    }

    @Test
    void size와_cursor를_그대로_Command에_전달한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(homeCommand(30, "abc"))).thenReturn(new RecommendationFeedResult(List.of(), null, false, null));

        mvc.perform(get("/api/v1/recommend/feed").param("size", "30").param("cursor", "abc")).andExpect(status().isOk());

        verify(recommendationService).getFeed(homeCommand(30, "abc"));
    }

    @Test
    void 지원하지_않는_surface는_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(get("/api/v1/recommend/feed").param("surface", "search")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void recommend_more의_필터와_정렬을_Command로_전달한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(any())).thenReturn(emptyFeed());

        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("sort", "newest").param("category", "에세이")
            .param("pubYearFrom", "2020").param("pubYearTo", "2024").param("matchScoreMin", "70").param("size", "20")
            .param("cursor", "abc")).andExpect(status().isOk());

        verify(recommendationService).getFeed(new GetRecommendationFeedCommand(42L, RecommendationFeedSurface.RECOMMEND_MORE, 20, "abc",
            RecommendationFeedSort.NEWEST, "에세이", 2020, 2024, 70));
    }

    @Test
    void recommend_more의_기본_정렬은_match다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(any())).thenReturn(emptyFeed());

        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more")).andExpect(status().isOk());

        verify(recommendationService).getFeed(recommendMoreCommand(RecommendationFeedSort.MATCH, null, null));
    }

    @Test
    void recommend_more의_출간연도는_시작이나_종료만_또는_둘_다_지정할_수_있다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(any())).thenReturn(emptyFeed());

        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("pubYearFrom", "2020"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("pubYearTo", "2024")).andExpect(status().isOk());
        mvc.perform(
            get("/api/v1/recommend/feed").param("surface", "recommend_more").param("pubYearFrom", "2020").param("pubYearTo", "2024"))
            .andExpect(status().isOk());

        verify(recommendationService).getFeed(recommendMoreCommand(RecommendationFeedSort.MATCH, 2020, null));
        verify(recommendationService).getFeed(recommendMoreCommand(RecommendationFeedSort.MATCH, null, 2024));
        verify(recommendationService).getFeed(recommendMoreCommand(RecommendationFeedSort.MATCH, 2020, 2024));
    }

    @Test
    void recommend_more는_newest와_price_asc_정렬도_받는다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(any())).thenReturn(emptyFeed());

        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("sort", "newest")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("sort", "price_asc")).andExpect(status().isOk());

        verify(recommendationService).getFeed(recommendMoreCommand(RecommendationFeedSort.NEWEST, null, null));
        verify(recommendationService).getFeed(recommendMoreCommand(RecommendationFeedSort.PRICE_ASC, null, null));
    }

    @Test
    void home에_recommend_more_전용_조건을_보내면_400을_응답한다() throws Exception {
        authenticateAs(42L);

        for (final String[] condition : new String[][] {{"sort", "match"}, {"category", "에세이"}, {"pubYearFrom", "2020"},
            {"pubYearTo", "2024"}, {"matchScoreMin", "70"}}) {
            mvc.perform(get("/api/v1/recommend/feed").param("surface", "home").param(condition[0], condition[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        }

        verifyNoInteractions(recommendationService);
    }

    @Test
    void 출간연도_범위가_뒤집히거나_지원하지_않는_정렬이나_범위_밖_매칭_점수는_400을_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(
            get("/api/v1/recommend/feed").param("surface", "recommend_more").param("pubYearFrom", "2024").param("pubYearTo", "2020"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("sort", "popular"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("matchScoreMin", "101"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/recommend/feed").param("surface", "recommend_more").param("matchScoreMin", "-1"))
            .andExpect(status().isBadRequest());

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
        when(recommendationService.getFeed(homeCommand(15, null)))
            .thenReturn(new RecommendationFeedResult(List.of(), null, false, "rule-only"));

        mvc.perform(get("/api/v1/recommend/feed")).andExpect(status().isOk()).andExpect(header().string("X-Degraded", "rule-only"));
    }

    @Test
    void 빈_목록도_200으로_응답한다() throws Exception {
        authenticateAs(42L);
        when(recommendationService.getFeed(homeCommand(15, null))).thenReturn(new RecommendationFeedResult(List.of(), null, true, null));

        mvc.perform(get("/api/v1/recommend/feed")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
            .andExpect(jsonPath("$.data.coldStart").value(true));
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static GetRecommendationFeedCommand homeCommand(final int size, final String cursor) {
        return new GetRecommendationFeedCommand(42L, RecommendationFeedSurface.HOME, size, cursor, null, null, null, null, null);
    }

    private static GetRecommendationFeedCommand recommendMoreCommand(final RecommendationFeedSort sort, final Integer pubYearFrom,
        final Integer pubYearTo) {
        return new GetRecommendationFeedCommand(42L, RecommendationFeedSurface.RECOMMEND_MORE, 15, null, sort, null, pubYearFrom, pubYearTo,
            null);
    }

    private static RecommendationFeedResult emptyFeed() {
        return new RecommendationFeedResult(List.of(), null, false, null);
    }
}
