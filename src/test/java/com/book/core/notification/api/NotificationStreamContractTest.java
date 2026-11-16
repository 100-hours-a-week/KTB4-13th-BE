package com.book.core.notification.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.core.notification.api.converter.NotificationCommandConverter;
import com.book.core.notification.api.converter.NotificationResultConverter;
import com.book.core.notification.api.converter.NotificationStreamResultConverter;
import com.book.core.notification.application.event.NotificationStreamMessage;
import com.book.core.notification.application.service.NotificationService;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import com.book.support.config.WebCorsConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import({NotificationCommandConverter.class, NotificationResultConverter.class, UserIdMvcConfig.class, SecurityConfiguration.class,
    JwtAuthenticationEntryPoint.class, WebCorsConfiguration.class, NotificationStreamManager.class, NotificationHeartbeatScheduler.class,
    NotificationStreamResultConverter.class, NotificationStreamContractTest.Meters.class})
@ActiveProfiles("test")
class NotificationStreamContractTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    MeterRegistry meters;
    @MockitoBean
    NotificationService service;
    @MockitoBean(name = "serviceJwtDecoder")
    JwtDecoder decoder;
    @MockitoBean
    CheckUserRestrictionUseCase checkUserRestrictionUseCase;

    @TestConfiguration(proxyBeanMethods = false)
    static class Meters {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Test
    void 인증된_SSE_연결에서_준비_신호와_알림_ID를_받는다() throws Exception {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        when(decoder.decode("test-token")).thenReturn(jwt);
        final AtomicReference<Consumer<NotificationStreamMessage>> receiver = new AtomicReference<>();
        final AtomicBoolean unsubscribed = new AtomicBoolean();
        when(service.subscribe(eq(42L), any())).thenAnswer(invocation -> {
            receiver.set(invocation.getArgument(1));
            return (Runnable) () -> unsubscribed.set(true);
        });
        final var result = mvc.perform(get("/api/v1/notifications/stream").header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk()).andExpect(request().asyncStarted()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("event:connected");
        assertThat(meters.get("notification.sse.connections").gauge().value()).isEqualTo(1);
        receiver.get().accept(NotificationStreamMessage.notification(99L));
        receiver.get().accept(NotificationStreamMessage.syncRequired());
        assertThat(result.getResponse().getContentAsString())
            .contains("event:notification", "\"notificationId\":99", "event:notification-sync-required", "required")
            .doesNotContain("id:99");
        result.getRequest().getAsyncContext().complete();
        assertThat(unsubscribed).isTrue();
        assertThat(meters.get("notification.sse.connections").gauge().value()).isZero();
    }
}
