package com.book.core.notification.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.support.config.WebCorsConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.book.core.notification.api.converter.NotificationCommandConverter;
import com.book.core.notification.api.converter.NotificationResultConverter;
import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.result.GetNotificationsResult;
import com.book.core.notification.application.result.NotificationItemResult;
import com.book.core.notification.application.service.NotificationService;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.notification.application.command.ReadNotificationCommand;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import({NotificationCommandConverter.class, NotificationResultConverter.class, UserIdMvcConfig.class, SecurityConfiguration.class,
    JwtAuthenticationEntryPoint.class, WebCorsConfiguration.class})
@ActiveProfiles("test")
class NotificationControllerTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    NotificationService service;
    @MockitoBean(name = "serviceJwtDecoder")
    JwtDecoder decoder;

    @MockitoBean
    CheckUserRestrictionUseCase checkUserRestrictionUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 요청_회원값을_무시하고_인증_회원의_알림만_조회한다() throws Exception {
        authenticate();
        final var command = new GetNotificationsCommand(42L);
        when(service.getRecent(command)).thenReturn(new GetNotificationsResult(
            List.of(new NotificationItemResult(10L, "제목", "본문", LocalDateTime.of(2026, 10, 19, 10, 0), null)), 25L));
        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer test-token").param("userId", "99"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.notifications[0].notificationId").value(10))
            .andExpect(jsonPath("$.data.notifications[0].title").value("제목"))
            .andExpect(jsonPath("$.data.notifications[0].body").value("본문")).andExpect(jsonPath("$.data.unreadCount").value(25));
        verify(service).getRecent(command);
    }

    @Test
    void 빈_목록을_정상_응답한다() throws Exception {
        authenticate();
        when(service.getRecent(new GetNotificationsCommand(42L))).thenReturn(new GetNotificationsResult(List.of(), 0L));
        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer test-token")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.notifications").isEmpty()).andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void 인증_없는_조회는_거부한다() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void 인증_회원으로_읽음_요청을_변환한다() throws Exception {
        authenticate();
        mvc.perform(patch("/api/v1/notifications/10/read").header("Authorization", "Bearer test-token").param("userId", "99"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).read(new ReadNotificationCommand(42L, 10L));
    }

    @Test
    void 타인과_없는_알림의_404_계약을_응답한다() throws Exception {
        authenticate();
        doThrow(new CoreException(ErrorCode.NOTIFICATION_NOT_FOUND)).when(service).read(new ReadNotificationCommand(42L, 10L));
        mvc.perform(patch("/api/v1/notifications/10/read").header("Authorization", "Bearer test-token")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void 음수_알림_ID는_거부한다() throws Exception {
        authenticate();
        mvc.perform(patch("/api/v1/notifications/-1/read").header("Authorization", "Bearer test-token")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void 인증_없는_읽음은_거부한다() throws Exception {
        mvc.perform(patch("/api/v1/notifications/10/read")).andExpect(status().isUnauthorized());
    }

    private void authenticate() {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(decoder.decode("test-token")).thenReturn(jwt);
    }
}
