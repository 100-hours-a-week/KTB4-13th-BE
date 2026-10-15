package com.book.core.coupon.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.service.OwnedCouponService;
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
@Import({CouponCommandConverter.class, UserIdMvcConfig.class, SecurityConfiguration.class, JwtAuthenticationEntryPoint.class,
    WebCorsConfiguration.class})
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
    void 인증이_없으면_발급이_거부된다() throws Exception {
        mvc.perform(post("/api/v1/coupons/10/issue")).andExpect(status().isUnauthorized());
        verifyNoInteractions(ownedCouponService);
    }

    private void authenticate() {
        final var jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build();
        when(jwtDecoder.decode("test-token")).thenReturn(jwt);
    }
}
