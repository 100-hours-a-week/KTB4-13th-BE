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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.compression.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
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
    @LocalServerPort
    private int port;
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
        mvc.perform(post(PATH + "/" + sessionId + "/messages").accept(MediaType.TEXT_EVENT_STREAM).contentType(MediaType.APPLICATION_JSON)
            .content(body("key", "질문"))).andExpect(status().isUnauthorized());
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

    @Test
    void OpenAPI에_같은_주소의_JSON과_SSE_응답을_함께_노출한다() throws Exception {
        final MvcResult response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        final JsonNode content = mapper.readTree(response.getResponse().getContentAsString()).path("paths")
            .path(PATH + "/{sessionId}/messages").path("post").path("responses").path("200").path("content");
        assertThat(content.path(MediaType.APPLICATION_JSON_VALUE).path("schema").isObject()).isTrue();
        assertThat(content.path(MediaType.TEXT_EVENT_STREAM_VALUE).path("schema").path("type").asText()).isEqualTo("string");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void SSE는_저장_전_delta를_전송하고_연결을_닫아도_최종_응답을_저장한다(final boolean disconnect) throws Exception {
        final long sessionId = create();
        final CountDownLatch release = new CountDownLatch(1);
        Mockito.doAnswer((final InvocationOnMock invocation) -> {
            final AiRecommendationChatRequest request = invocation.getArgument(0);
            final Consumer<String> onDelta = invocation.getArgument(1);
            onDelta.accept("말풍선");
            assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            onDelta.accept(" 계속");
            return new AiRecommendationChatResult(request.spec(), "최종 답변", List.of(), null, List.of(), false);
        }).when(ai).chat(any(), any());

        try (final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
            final HttpResponse<InputStream> response =
                client.send(streamRequest(sessionId, "stream"), HttpResponse.BodyHandlers.ofInputStream());
            try (final BufferedReader events = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE))
                    .hasValueSatisfying((final String value) -> assertThat(value).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE));
                assertThat(response.headers().firstValue(HttpHeaders.CONTENT_ENCODING)).isEmpty();
                assertThat(readEvent(events, "delta").path("text").asText()).isEqualTo("말풍선");
                assertThat(release.getCount()).isEqualTo(1);
                if (!disconnect) {
                    release.countDown();
                    assertThat(readEvent(events, "delta").path("text").asText()).isEqualTo(" 계속");
                    assertThat(readEvent(events, "done").path("data").path("reply").asText()).isEqualTo("최종 답변");
                    assertThat(events.readLine()).isNull();
                }
            }
        } finally {
            release.countDown();
        }

        // 연결 종료 이후에도 서버의 저장이 끝날 때까지 기다립니다.
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        MvcResult history;
        do {
            history = mvc.perform(get(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk()).andReturn();
            if (mapper.readTree(history.getResponse().getContentAsString()).path("data").path("messages").size() == 2) {
                break;
            }
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        jsonPath("$.data.messages[1].response.reply").value("최종 답변").match(history);
        verify(ai, times(1)).chat(any(), any());
    }

    @Test
    void SSE_AI_실패는_기존_오류_본문으로_전송하고_같은_키에서_재생한다() throws Exception {
        final long sessionId = create();
        Mockito.doThrow(new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT)).when(ai).chat(any(), any());
        try (final HttpClient client = HttpClient.newHttpClient()) {
            final HttpResponse<InputStream> response =
                client.send(streamRequest(sessionId, "failed"), HttpResponse.BodyHandlers.ofInputStream());
            assertThat(response.statusCode()).isEqualTo(200);
            try (final BufferedReader events = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                final var error = readEvent(events, "error");
                assertThat(error.path("success").asBoolean()).isFalse();
                assertThat(error.path("code").asText()).isEqualTo(ErrorCode.AI_RECOMMENDATION_TIMEOUT.code());
                assertThat(error.path("traceId").asText()).isEqualTo("chat-sse-test");
                assertThat(events.readLine()).isNull();
            }
        }
        mvc.perform(post(PATH + "/" + sessionId + "/messages").header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON).content(body("failed", "질문"))).andExpect(status().isGatewayTimeout());
        verify(ai, times(1)).chat(any(), any());
        Mockito.verify(ai, Mockito.never()).chat(any());
    }

    private HttpRequest streamRequest(final long sessionId, final String key) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH + "/" + sessionId + "/messages"))
            .timeout(Duration.ofSeconds(15)).header(HttpHeaders.AUTHORIZATION, authorization)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE).header(HttpHeaders.ACCEPT_ENCODING, "gzip")
            .header("X-Request-Id", "chat-sse-test").POST(HttpRequest.BodyPublishers.ofString(body(key, "질문"))).build();
    }

    private JsonNode readEvent(final BufferedReader events, final String name) throws Exception {
        assertThat(events.readLine()).isEqualTo("event:" + name);
        final String data = events.readLine();
        assertThat(data).startsWith("data:");
        assertThat(events.readLine()).isEmpty();
        return mapper.readTree(data.substring("data:".length()));
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
