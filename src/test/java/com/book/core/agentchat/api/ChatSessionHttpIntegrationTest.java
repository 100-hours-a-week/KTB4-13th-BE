package com.book.core.agentchat.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.recommendation.application.port.AiRecommendationChatRequest;
import com.book.core.recommendation.application.port.AiRecommendationChatResult;
import com.book.core.recommendation.application.port.AiRecommendationClient;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class ChatSessionHttpIntegrationTest {
    private static final String PATH = "/api/v1/recommend/chat/sessions";
    private static final AtomicLong NEXT_ID = new AtomicLong(2800000L);
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    private MockMvc mvc;
    @Autowired
    private TokenIssuer tokens;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper mapper;
    @MockitoBean
    private AiRecommendationClient ai;
    private long userId;
    private String authorization;

    @BeforeEach
    void setUp() {
        userId = NEXT_ID.incrementAndGet();
        jdbc.update("INSERT INTO users (id, nickname) VALUES (?, ?)", userId, "http" + userId);
        authorization = "Bearer " + tokens.issue(userId).accessToken();
        Mockito.doAnswer((final InvocationOnMock invocation) -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            final AiRecommendationChatRequest request = invocation.getArgument(0);
            assertThat(request.userId()).isEqualTo(userId);
            return new AiRecommendationChatResult(request.spec(), "답변", List.of(), null, List.of("다음"), false);
        }).when(ai).chat(any());
    }

    @Test
    void 실제_인증과_HTTP로_생성_전송_재시도_재열람까지_연결한다() throws Exception {
        final long sessionId = create();
        mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON).content(body("key", "질문"))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reply").value("답변"));
        mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON).content(body("key", "질문"))).andExpect(status().isOk());
        mvc.perform(get(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages.length()").value(2)).andExpect(jsonPath("$.data.messages[0].text").value("질문"))
            .andExpect(jsonPath("$.data.messages[1].response.reply").value("답변"))
            .andExpect(jsonPath("$.data.messages[1].createdAt").isNotEmpty());
        verify(ai, times(1)).chat(any());
    }

    @Test
    void 대화방_목록은_내_방만_생성순_커서로_반환한다() throws Exception {
        final long first = create();
        final long second = create();
        final String other = "Bearer " + tokens.issue(userId + 1).accessToken();
        mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, other)).andExpect(status().isCreated());
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, authorization).param("limit", "1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessions.length()").value(1)).andExpect(jsonPath("$.data.sessions[0].sessionId").value(second))
            .andExpect(jsonPath("$.data.nextBeforeId").value(second));
        mvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, authorization).param("limit", "1").param("beforeId", Long.toString(second)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.sessions[0].sessionId").value(first))
            .andExpect(jsonPath("$.data.nextBeforeId").isEmpty());
    }

    @Test
    void 비로그인과_타인_대화방을_거절한다() throws Exception {
        mvc.perform(post(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        final long sessionId = create();
        final String other = "Bearer " + tokens.issue(userId + 1).accessToken();
        mvc.perform(get(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, other)).andExpect(status().isNotFound());
        mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, other)
            .contentType(MediaType.APPLICATION_JSON).content(body("key", "질문"))).andExpect(status().isNotFound());
    }

    @Test
    void AI_실패는_HTTP_오류로_전달하고_대화에서_제외한다() throws Exception {
        final long sessionId = create();
        Mockito.doThrow(new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT)).when(ai).chat(any());
        mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON).content(body("failed", "실패 질문"))).andExpect(status().isGatewayTimeout());
        mvc.perform(get(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messages.length()").value(0));
    }

    @Test
    void 후속_전송은_저장한_서버_이력을_사용한다() throws Exception {
        final long sessionId = create();
        mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON).content(body("first", "처음 질문"))).andExpect(status().isOk());
        Mockito.doAnswer((final InvocationOnMock invocation) -> {
            final AiRecommendationChatRequest request = invocation.getArgument(0);
            assertThat(request.userId()).isEqualTo(userId);
            assertThat(request.message()).isEqualTo("다음 질문");
            assertThat(request.recentTurns()).hasSize(2);
            assertThat(request.recentTurns().getFirst().text()).isEqualTo("처음 질문");
            return new AiRecommendationChatResult(request.spec(), "이어진 답변", List.of(), null, List.of(), false);
        }).when(ai).chat(any());
        mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON).content(body("second", "다음 질문"))).andExpect(status().isOk());
    }

    @Test
    void 같은_방의_동시_새_전송은_HTTP409로_거절한다() throws Exception {
        final long sessionId = create();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        Mockito.doAnswer((final InvocationOnMock invocation) -> {
            entered.countDown();
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            final AiRecommendationChatRequest request = invocation.getArgument(0);
            return new AiRecommendationChatResult(request.spec(), "답변", List.of(), null, List.of(), false);
        }).when(ai).chat(any());
        try (final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final Future<MvcResult> first =
                executor
                    .submit(
                        () -> mvc
                            .perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
                                .contentType(MediaType.APPLICATION_JSON).content(body("first", "처음")))
                            .andExpect(status().isOk()).andReturn());
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
                    .contentType(MediaType.APPLICATION_JSON).content(body("second", "동시"))).andExpect(status().isConflict());
            } finally {
                release.countDown();
            }
            first.get(10, TimeUnit.SECONDS);
        }
    }

    private long create() throws Exception {
        final MvcResult result =
            mvc.perform(post(PATH).header(HttpHeaders.AUTHORIZATION, authorization)).andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).path("data").path("sessionId").asLong();
    }

    private String body(final String key, final String message) {
        return "{\"idempotencyKey\":\"" + key + "\",\"message\":\"" + message + "\",\"consented\":true}";
    }
}
