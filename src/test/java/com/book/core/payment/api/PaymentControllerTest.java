package com.book.core.payment.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.api.config.SwaggerConfig;
import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import com.book.core.payment.api.converter.PaymentCommandConverter;
import com.book.core.payment.application.command.ApprovePaymentCommand;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.command.FailPaymentCommand;
import com.book.core.payment.application.service.PaymentService;
import com.book.core.payment.application.result.PaymentCreationResult;
import com.book.support.config.WebCorsConfiguration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import({PaymentCommandConverter.class, UserIdMvcConfig.class, SecurityConfiguration.class, JwtAuthenticationEntryPoint.class,
    WebCorsConfiguration.class, SwaggerConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class})
@ActiveProfiles("test")
class PaymentControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean(name = "serviceJwtDecoder")
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CheckUserRestrictionUseCase checkUserRestrictionUseCase;

    @Test
    void 일반_결제는_JWT_회원과_미입력_기본값으로_생성한다() throws Exception {
        authenticate();
        final CreatePaymentCommand command = new CreatePaymentCommand(42L, "order", -1L, BigDecimal.valueOf(-1), "request-key");
        when(paymentService.create(command)).thenReturn(new PaymentCreationResult(3L, "payment_test"));
        mvc.perform(post("/api/v1/payments").header("Authorization", "Bearer test-token").header("Idempotency-Key", "request-key")
            .contentType(MediaType.APPLICATION_JSON).content("{\"orderKey\":\"order\"}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.paymentId").value(3))
            .andExpect(jsonPath("$.data.pgOrderId").value("payment_test"));
        verify(paymentService).create(command);
    }

    @Test
    void 명시한_쿠폰과_포인트_값은_그대로_전달한다() throws Exception {
        authenticate();
        final CreatePaymentCommand command = new CreatePaymentCommand(42L, "order", 10L, BigDecimal.valueOf(1200), "request-key");
        when(paymentService.create(command)).thenReturn(new PaymentCreationResult(3L, "payment_test"));
        mvc.perform(post("/api/v1/payments").header("Authorization", "Bearer test-token").header("Idempotency-Key", "request-key")
            .contentType(MediaType.APPLICATION_JSON).content("{\"orderKey\":\"order\",\"useOwnedCouponId\":10,\"usePoint\":1200}"))
            .andExpect(status().isOk());
        verify(paymentService).create(command);
    }

    @Test
    void 결제_생성은_멱등키가_없으면_안전하게_거부한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/payments").header("Authorization", "Bearer test-token").contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderKey\":\"order\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService);
    }

    @Test
    void 성공_콜백은_주문키와_PG키_금액을_변환하고_데이터_없이_응답한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/payments/callback/success").header("Authorization", "Bearer test-token").param("orderKey", "order")
            .param("paymentKey", "test-pg-key").param("amount", "8800.00")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").doesNotExist());
        verify(paymentService).successForUser(42L, new ApprovePaymentCommand("order", "test-pg-key", new BigDecimal("8800.00")));
    }

    @Test
    void 실패_콜백은_확인중_응답을_반환한다() throws Exception {
        authenticate();
        doThrow(new CoreException(ErrorCode.PG_PENDING)).when(paymentService).fail(42L,
            new FailPaymentCommand("order", "FAIL", "test failure"));
        mvc.perform(post("/api/v1/payments/callback/fail").header("Authorization", "Bearer test-token").param("orderKey", "order")
            .param("code", "FAIL").param("message", "test failure")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value("PG_PENDING"));
        verify(paymentService).fail(42L, new FailPaymentCommand("order", "FAIL", "test failure"));
    }

    @Test
    void 확정된_0원_결제_실패_콜백은_정상_응답한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/payments/callback/fail").header("Authorization", "Bearer test-token").param("orderKey", "order")
            .param("code", "FAIL").param("message", "test failure")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
        verify(paymentService).fail(42L, new FailPaymentCommand("order", "FAIL", "test failure"));
    }

    @Test
    void 다른_회원의_콜백은_권한_없음으로_반환한다() throws Exception {
        authenticate();
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(paymentService).successForUser(42L,
            new ApprovePaymentCommand("order", "test-pg-key", BigDecimal.TEN));
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(paymentService).fail(42L,
            new FailPaymentCommand("order", "FAIL", "test failure"));

        mvc.perform(post("/api/v1/payments/callback/success").header("Authorization", "Bearer test-token").param("orderKey", "order")
            .param("paymentKey", "test-pg-key").param("amount", "10")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/payments/callback/fail").header("Authorization", "Bearer test-token").param("orderKey", "order")
            .param("code", "FAIL").param("message", "test failure")).andExpect(status().isForbidden());
    }

    @Test
    void 필수_키_누락과_공백_주문키는_HTTP에서_거부한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/payments").header("Authorization", "Bearer test-token").header("Idempotency-Key", "request-key")
            .contentType(MediaType.APPLICATION_JSON).content("{\"orderKey\":\" \"}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(post("/api/v1/payments/callback/success").header("Authorization", "Bearer test-token").param("orderKey", "order"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService);
    }

    @Test
    void 결제_금액_불일치는_참고_오류_코드로_응답한다() throws Exception {
        authenticate();
        doThrow(new CoreException(ErrorCode.PAYMENT_AMOUNT_MISMATCH)).when(paymentService).successForUser(42L,
            new ApprovePaymentCommand("order", "test-pg-key", BigDecimal.TEN));
        mvc.perform(post("/api/v1/payments/callback/success").header("Authorization", "Bearer test-token").param("orderKey", "order")
            .param("paymentKey", "test-pg-key").param("amount", "10")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E2001"));
    }

    @Test
    void Book_기존_정책에_따라_새_결제_API는_인증이_필요하다() throws Exception {
        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content("{\"orderKey\":\"order\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/payments/callback/success").param("orderKey", "order").param("paymentKey", "test").param("amount", "10"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(paymentService);
    }

    @Test
    void OpenAPI에_일반_결제_성공_실패_계약을_노출한다() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/payments'].post").exists())
            .andExpect(jsonPath("$.paths['/api/v1/payments'].post.parameters[0].name").value("Idempotency-Key"))

            .andExpect(jsonPath("$.paths['/api/v1/payments/callback/success'].post").exists())
            .andExpect(jsonPath("$.paths['/api/v1/payments/callback/fail'].post").exists())
            .andExpect(jsonPath("$.paths['/api/v1/payments/callback/fail'].post.responses['409']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/payments/callback/success'].post.parameters[*].name",
                containsInAnyOrder("orderKey", "paymentKey", "amount")))
            .andExpect(jsonPath("$.paths['/api/v1/payments/callback/fail'].post.parameters[*].name",
                containsInAnyOrder("orderKey", "code", "message")));
    }

    @Test
    void 성공_콜백은_이전_orderId_파라미터만_보내면_거부한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/payments/callback/success").header("Authorization", "Bearer test-token").param("orderId", "order")
            .param("paymentKey", "test-pg-key").param("amount", "10")).andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService);
    }

    @Test
    void 실패_콜백은_이전_orderId_파라미터만_보내면_거부한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/payments/callback/fail").header("Authorization", "Bearer test-token").param("orderId", "order")
            .param("code", "FAIL").param("message", "test failure")).andExpect(status().isBadRequest());
        verifyNoInteractions(paymentService);
    }

    private void authenticate() {
        when(jwtDecoder.decode("test-token")).thenReturn(Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build());
    }
}
