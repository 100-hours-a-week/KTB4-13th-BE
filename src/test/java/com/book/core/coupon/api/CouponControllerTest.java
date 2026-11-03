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
import com.book.common.api.config.SwaggerConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.support.config.WebCorsConfiguration;
import com.book.core.coupon.api.converter.CouponCommandConverter;
import com.book.core.coupon.api.converter.CouponResultConverter;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.command.GetOwnedCouponsCommand;
import com.book.core.coupon.application.command.GetCouponsCommand;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.service.OwnedCouponService;
import com.book.core.coupon.application.service.CouponService;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

@WebMvcTest(CouponController.class)
@Import({CouponCommandConverter.class, CouponResultConverter.class, UserIdMvcConfig.class, SecurityConfiguration.class,
    JwtAuthenticationEntryPoint.class, WebCorsConfiguration.class, SwaggerConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class})
@ActiveProfiles("test")
class CouponControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OwnedCouponService ownedCouponService;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean(name = "serviceJwtDecoder")
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CheckUserRestrictionUseCase checkUserRestrictionUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 쿠폰_ID_목록으로_인증_없이_쿠폰_상세를_조회한다() throws Exception {
        final var coupon = new CouponResult(10L, "할인", CouponType.PERCENT_RATE, new BigDecimal("0.10"), new BigDecimal("1000"),
            LocalDateTime.of(2020, 1, 1, 9, 0));
        when(couponService.getCoupons(new GetCouponsCommand(List.of(10L, 20L)))).thenReturn(List.of(coupon));

        mvc.perform(get("/api/v1/coupons").param("ids", "10,20")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].id").value(10))
            .andExpect(jsonPath("$.data[0].name").value("할인")).andExpect(jsonPath("$.data[0].type").value("PERCENT_RATE"))
            .andExpect(jsonPath("$.data[0].discount").value(0.10)).andExpect(jsonPath("$.data[0].minOrderAmount").value(1000))
            .andExpect(jsonPath("$.data[0].expiredAt").value("2020-01-01T09:00:00")).andExpect(jsonPath("$.data[0].userId").doesNotExist())
            .andExpect(jsonPath("$.data[0].usedCount").doesNotExist());
        verify(couponService).getCoupons(new GetCouponsCommand(List.of(10L, 20L)));
        verifyNoInteractions(ownedCouponService);
    }

    @Test
    void 조회할_활성_쿠폰이_없으면_빈_목록을_반환한다() throws Exception {
        when(couponService.getCoupons(new GetCouponsCommand(List.of(999L)))).thenReturn(List.of());
        mvc.perform(get("/api/v1/coupons").param("ids", "999")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "10,not-a-number", "10,,20"})
    void 잘못된_쿠폰_ID_목록은_HTTP_경계에서_거부한다(final String ids) throws Exception {
        mvc.perform(get("/api/v1/coupons").param("ids", ids)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));
        verifyNoInteractions(couponService, ownedCouponService);
    }

    @Test
    void 쿠폰_ID_목록이_없으면_거부한다() throws Exception {
        mvc.perform(get("/api/v1/coupons")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        verifyNoInteractions(couponService, ownedCouponService);
    }

    @Test
    void 쿠폰_조회는_공개_명세이고_발급과_보유_조회는_인증_명세다() throws Exception {
        mvc.perform(get("/v3/api-docs/general")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/coupons'].get.parameters[0].name").value("ids"))
            .andExpect(jsonPath("$.paths['/api/v1/coupons'].get.parameters[0].required").value(true))
            .andExpect(jsonPath("$.paths['/api/v1/coupons'].get.security").doesNotExist())
            .andExpect(jsonPath("$.paths['/api/v1/coupons/{couponId}/issue'].post.security[0].bearerAuth").exists())
            .andExpect(jsonPath("$.paths['/api/v1/coupons/{couponId}/issue'].post.responses['400'].description")
                .value(org.hamcrest.Matchers.containsString("발급 한도 초과(E4003)")))
            .andExpect(jsonPath("$.paths['/api/v1/owned-coupons'].get.security[0].bearerAuth").exists())
            .andExpect(jsonPath("$.components.schemas.CouponResponse.properties.id.type").value("integer"))
            .andExpect(jsonPath("$.components.schemas.CouponResponse.properties.usedCount").doesNotExist());
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
        doThrow(new CoreException(ErrorCode.COUPON_ALREADY_ISSUED)).when(ownedCouponService).issue(new IssueCouponCommand(42L, 10L));
        mvc.perform(post("/api/v1/coupons/10/issue").header("Authorization", "Bearer test-token")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E4001"));
        doThrow(new CoreException(ErrorCode.COUPON_NOT_FOUND_OR_EXPIRED)).when(ownedCouponService).issue(new IssueCouponCommand(42L, 11L));
        mvc.perform(post("/api/v1/coupons/11/issue").header("Authorization", "Bearer test-token")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E4000"));
    }

    @Test
    void 발급_한도_초과는_오류_코드를_반환한다() throws Exception {
        authenticate();
        doThrow(new CoreException(ErrorCode.COUPON_SOLD_OUT)).when(ownedCouponService).issue(new IssueCouponCommand(42L, 12L));
        mvc.perform(post("/api/v1/coupons/12/issue").header("Authorization", "Bearer test-token")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E4003"));
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
