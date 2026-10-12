package com.book.core.point.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.api.converter.PointCommandConverter;
import com.book.core.point.api.converter.PointResultConverter;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.result.PointHistoryResult;
import com.book.core.point.application.service.PointService;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointController.class)
@Import({PointCommandConverter.class, PointResultConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class PointControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PointService pointService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_회원으로_조회하고_기존_응답_구조를_유지한다() throws Exception {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(pointService.getPoint(new GetPointCommand(42L))).thenReturn(new GetPointResult(42L, new BigDecimal("1250"),
            List.of(new PointHistoryResult(2L, 42L, PointType.REVIEW, 10L, new BigDecimal("250"), LocalDateTime.of(2026, 10, 12, 12, 0)))));

        mvc.perform(get("/api/v1/point").param("userId", "99")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.balance.userId").value(42)).andExpect(jsonPath("$.data.balance.balance").value(1250))
            .andExpect(jsonPath("$.data.histories[0].id").value(2)).andExpect(jsonPath("$.data.histories[0].userId").value(42))
            .andExpect(jsonPath("$.data.histories[0].type").value("REVIEW"))
            .andExpect(jsonPath("$.data.histories[0].referenceId").value(10)).andExpect(jsonPath("$.data.histories[0].amount").value(250))
            .andExpect(jsonPath("$.data.histories[0].createdAt").exists())
            .andExpect(jsonPath("$.data.histories[0].balanceAfter").doesNotExist());
        verify(pointService).getPoint(new GetPointCommand(42L));
    }

    @Test
    void 잔액이_없으면_기존_404_계약을_반환한다() throws Exception {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(pointService.getPoint(any(GetPointCommand.class))).thenThrow(new CoreException(ErrorCode.NOT_FOUND_DATA));

        mvc.perform(get("/api/v1/point")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"));
    }

    @Test
    void 기술_오류는_안전한_공통_오류로_반환한다() throws Exception {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(pointService.getPoint(any(GetPointCommand.class))).thenThrow(new DataAccessResourceFailureException("database detail"));

        mvc.perform(get("/api/v1/point")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }
}
