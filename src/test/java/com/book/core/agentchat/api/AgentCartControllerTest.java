package com.book.core.agentchat.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.api.converter.AgentCartCommandConverter;
import com.book.core.agentchat.api.converter.AgentCartResultConverter;
import com.book.core.agentchat.application.command.ConfirmCartProposalCommand;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.application.service.AgentCartService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

@WebMvcTest(AgentCartController.class)
@Import({AgentCartCommandConverter.class, AgentCartResultConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class AgentCartControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private AgentCartService service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate() {
        final Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("7").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void 첫_실행은_201_재생은_200이며_사용자는_인증에서_고정한다() throws Exception {
        authenticate();
        when(service.confirm(any())).thenReturn(new CartChangeResult(21L, 11L, List.of(), Instant.EPOCH, null, false),
            new CartChangeResult(21L, 11L, List.of(), Instant.EPOCH, null, true));
        final String body = "{\"proposalId\":11,\"idempotencyKey\":\"same\"}";
        mvc.perform(post("/api/v1/agent/cart/executions").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/agent/cart/executions/21"));
        mvc.perform(post("/api/v1/agent/cart/executions").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
            .andExpect(header().string("Location", "/api/v1/agent/cart/executions/21"));
        verify(service, Mockito.times(2)).confirm(new ConfirmCartProposalCommand(7L, 11L, "same"));
    }

    @Test
    void 타인의_제안은_404이며_입력_오류는_400이다() throws Exception {
        authenticate();
        when(service.getProposal(any())).thenThrow(new CoreException(ErrorCode.CART_CHANGE_NOT_FOUND));
        mvc.perform(get("/api/v1/agent/cart/proposals/11")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/agent/cart/executions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"proposalId\":0,\"idempotencyKey\":\"\"}")).andExpect(status().isBadRequest());
    }
}
