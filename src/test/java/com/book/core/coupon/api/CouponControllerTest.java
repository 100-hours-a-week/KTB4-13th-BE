package com.book.core.coupon.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.support.config.WebCorsConfiguration;
import com.book.core.coupon.api.converter.CouponCommandConverter;
import com.book.core.coupon.api.converter.CouponResultConverter;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.command.GetOwnedCouponsCommand;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.service.OwnedCouponService;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponController.class)
@Import({CouponCommandConverter.class, CouponResultConverter.class, UserIdMvcConfig.class, SecurityConfiguration.class,
    JwtAuthenticationEntryPoint.class, WebCorsConfiguration.class})
@ActiveProfiles("test")
class CouponControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OwnedCouponService ownedCouponService;

    @MockitoBean(name = "serviceJwtDecoder")
    private JwtDecoder jwtDecoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 발급은_JWT_회원_ID를_쓰고_빈_성공_응답을_반환한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/coupons/10/issue").header("Authorization", "Bearer test-token").param("userId", "99"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").doesNotExist());
        verify(ownedCouponService).issue(new IssueCouponCommand(42L, 10L));
    }

    @Test
    void 보유_응답은_참고_프로젝트의_flat_필드를_유지한다() throws Exception {
        authenticate();
        final var coupon = new CouponResult(10L, "할인", CouponType.PERCENT_RATE, new BigDecimal("0.10"), new BigDecimal("1000"),
            LocalDateTime.of(2026, 10, 20, 9, 0));
        when(ownedCouponService.getOwnedCoupons(new GetOwnedCouponsCommand(42L)))
            .thenReturn(List.of(new OwnedCouponResult(1L, 42L, OwnedCouponStatus.ISSUED, 2L, 1L, coupon)));
        mvc.perform(get("/api/v1/owned-coupons").header("Authorization", "Bearer test-token").param("userId", "99"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(1)).andExpect(jsonPath("$.data[0].state").value("ISSUED"))
            .andExpect(jsonPath("$.data[0].name").value("할인")).andExpect(jsonPath("$.data[0].type").value("PERCENT_RATE"))
            .andExpect(jsonPath("$.data[0].discount").value(0.10)).andExpect(jsonPath("$.data[0].minOrderAmount").value(1000))
            .andExpect(jsonPath("$.data[0].maxUseCount").value(2)).andExpect(jsonPath("$.data[0].usedCount").value(1))
            .andExpect(jsonPath("$.data[0].expiredAt").exists()).andExpect(jsonPath("$.data[0].coupon").doesNotExist())
            .andExpect(jsonPath("$.data[0].userId").doesNotExist());
    }

    @Test
    void 보유_쿠폰이_없으면_빈_배열을_반환한다() throws Exception {
        authenticate();
        when(ownedCouponService.getOwnedCoupons(new GetOwnedCouponsCommand(42L))).thenReturn(List.of());
        mvc.perform(get("/api/v1/owned-coupons").header("Authorization", "Bearer test-token")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void 잘못된_쿠폰_ID는_HTTP_경계에서_거부한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/coupons/0/issue").header("Authorization", "Bearer test-token")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(post("/api/v1/coupons/not-a-number/issue").header("Authorization", "Bearer test-token"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(ownedCouponService);
    }

    @Test
    void 중복_발급과_만료는_참고_오류_코드를_반환한다() throws Exception {
        authenticate();
        doThrow(new CoreException(ErrorCode.COUPON_ALREADY_DOWNLOADED)).when(ownedCouponService).issue(new IssueCouponCommand(42L, 10L));
        mvc.perform(post("/api/v1/coupons/10/issue").header("Authorization", "Bearer test-token")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E4001"));
        doThrow(new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED)).when(ownedCouponService).issue(new IssueCouponCommand(42L, 11L));
        mvc.perform(post("/api/v1/coupons/11/issue").header("Authorization", "Bearer test-token")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E4000"));
    }

    @Test
    void 인증이_없으면_발급과_보유_조회가_거부된다() throws Exception {
        mvc.perform(get("/api/v1/owned-coupons")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/coupons/10/issue")).andExpect(status().isUnauthorized());
        verifyNoInteractions(ownedCouponService);
    }

    private void authenticate() {
        final var jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build();
        when(jwtDecoder.decode("test-token")).thenReturn(jwt);
    }
}
