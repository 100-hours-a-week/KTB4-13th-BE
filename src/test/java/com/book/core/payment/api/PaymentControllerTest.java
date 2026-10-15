package com.book.core.payment.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.api.config.SwaggerConfig;
import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.core.payment.api.converter.PaymentCommandConverter;
import com.book.core.payment.application.command.CreatePaymentCommand;
import com.book.core.payment.application.service.PaymentService;
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

    @Test
    void 일반_결제는_JWT_회원과_미입력_기본값으로_생성한다() throws Exception {
        authenticate();
        final var command = new CreatePaymentCommand(42L, "order", -1L, BigDecimal.valueOf(-1));
        when(paymentService.create(command)).thenReturn(3L);
        mvc.perform(post("/api/v1/payments").header("Authorization", "Bearer test-token").contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderKey\":\"order\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.paymentId").value(3));
        verify(paymentService).create(command);
    }

    @Test
    void 명시한_쿠폰과_포인트_값은_그대로_전달한다() throws Exception {
        authenticate();
        final var command = new CreatePaymentCommand(42L, "order", 10L, BigDecimal.valueOf(1200));
        when(paymentService.create(command)).thenReturn(3L);
        mvc.perform(post("/api/v1/payments").header("Authorization", "Bearer test-token").contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderKey\":\"order\",\"useOwnedCouponId\":10,\"usePoint\":1200}")).andExpect(status().isOk());
        verify(paymentService).create(command);
    }

    @Test
    void Book_기존_정책에_따라_새_결제_API는_인증이_필요하다() throws Exception {
        mvc.perform(post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON).content("{\"orderKey\":\"order\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/payments/callback/success").param("orderId", "order").param("paymentKey", "test").param("amount", "10"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(paymentService);
    }

    private void authenticate() {
        when(jwtDecoder.decode("test-token")).thenReturn(Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build());
    }
}
