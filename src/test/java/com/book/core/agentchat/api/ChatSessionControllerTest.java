package com.book.core.agentchat.api;

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
import com.book.core.agentchat.api.converter.ChatSessionCommandConverter;
import com.book.core.agentchat.api.converter.ChatSessionResultConverter;
import com.book.core.agentchat.application.command.GetChatHistoryCommand;
import com.book.core.agentchat.application.command.GetChatSessionsCommand;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.result.AgentExecutionSummary;
import com.book.core.agentchat.application.result.ChatHistoryMessage;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.result.ChatSessionPageResult;
import com.book.core.agentchat.application.result.ChatSessionSummary;
import com.book.core.agentchat.application.service.ChatSessionService;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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

@WebMvcTest(ChatSessionController.class)
@Import({ChatSessionCommandConverter.class, ChatSessionResultConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class ChatSessionControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ChatSessionService service;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        final Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void 인증한_사용자의_대화방을_생성한다() throws Exception {
        authenticate();
        when(service.create(any())).thenReturn(11L);
        mvc.perform(post("/api/v1/recommend/chat/sessions")).andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/recommend/chat/sessions/11")).andExpect(jsonPath("$.data.sessionId").value(11));
    }

    @Test
    void 대화_본문은_명령으로_옮기고_추천_응답을_변환한다() throws Exception {
        authenticate();
        when(service.send(any())).thenReturn(new ChatResponseSnapshot(1, Map.of("intent", "semantic"), "답변", List.of(), "다음 질문",
            List.of("다른 책"), false, null, List.of(), null));
        mvc.perform(post("/api/v1/recommend/chat/sessions/11/messages").contentType(MediaType.APPLICATION_JSON).content("""
            {"idempotencyKey":"key", "message":"질문", "consented":true, "excludeBookIds":[12]}
            """)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.data.reply").value("답변")).andExpect(jsonPath("$.data.buttons[0]").value("다른 책"));
        verify(service).send(new SendChatMessageCommand(7L, 11L, "key", "질문", true, List.of(12L)));
    }

    @Test
    void 변경_거절도_채팅은_200이며_실제_안내와_객체_버튼_계약을_반환한다() throws Exception {
        authenticate();
        when(service.send(any())).thenReturn(ChatResponseSnapshot.shop(Map.of(), "재고가 부족해 담지 못했습니다", List.of(),
            new AgentExecutionSummary(null, null, "rejected", "INSUFFICIENT_PRODUCT_STOCK")));
        mvc.perform(post("/api/v1/recommend/chat/sessions/11/messages").contentType(MediaType.APPLICATION_JSON).content("""
            {"idempotencyKey":"stock", "message":"5권 담아줘", "consented":true, "excludeBookIds":[]}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.data.route").value("shop"))
            .andExpect(jsonPath("$.data.reply").value("재고가 부족해 담지 못했습니다")).andExpect(jsonPath("$.data.execution.status").value("rejected"))
            .andExpect(jsonPath("$.data.cards").doesNotExist()).andExpect(jsonPath("$.data.spec").doesNotExist());
    }

    @Test
    void 조회_커서를_명령으로_옮기고_목록과_이력을_변환한다() throws Exception {
        authenticate();
        when(service.list(any())).thenReturn(new ChatSessionPageResult(List.of(new ChatSessionSummary(11L, null)), 11L));
        when(service.history(any())).thenReturn(List.of(new ChatHistoryMessage(1L, 3L, "user", "질문", null, null)));
        mvc.perform(get("/api/v1/recommend/chat/sessions").param("beforeId", "20").param("limit", "1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.nextBeforeId").value(11));
        mvc.perform(get("/api/v1/recommend/chat/sessions/11/messages").param("afterSequence", "2").param("limit", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.messages[0].sequence").value(3))
            .andExpect(jsonPath("$.data.nextAfterSequence").value(3));
        verify(service).list(new GetChatSessionsCommand(7L, 20L, 1));
        verify(service).history(new GetChatHistoryCommand(7L, 11L, 2, 1));
    }

    @Test
    void 유효하지_않은_본문과_경로는_업무_처리_전에_거절한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/recommend/chat/sessions/11/messages").contentType(MediaType.APPLICATION_JSON)
            .content("{\"idempotencyKey\":\"key\",\"message\":\"질문\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/recommend/chat/sessions/0/messages").contentType(MediaType.APPLICATION_JSON)
            .content("{\"idempotencyKey\":\"key\",\"message\":\"질문\",\"consented\":true}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/recommend/chat/sessions").param("limit", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/recommend/chat/sessions/11/messages").param("afterSequence", "-1")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
