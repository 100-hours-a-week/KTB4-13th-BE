package com.book.core.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.api.converter.OrderCommandConverter;
import com.book.core.order.api.converter.OrderResultConverter;
import com.book.core.order.application.command.CancelOrderCommand;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.result.CreateOrderItemResult;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.service.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(OrderController.class)
@Import({OrderCommandConverter.class, OrderResultConverter.class, OrderControllerTest.AuthenticationPrincipalTestConfig.class})
@ActiveProfiles("test")
class OrderControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService orderService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkout_경로에서_인증된_userId와_요청을_Command로_변환해_기존_응답_외피로_반환한다() throws Exception {
        authenticateAs(42L);
        when(orderService.createOrder(any())).thenReturn(result(true));

        mvc.perform(post("/api/v1/orders/checkout").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "items": [{"itemId": 701, "quantity": 2}]
                                }
                                """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderKey").value("order_test")).andExpect(jsonPath("$.data.status").value("ORDER_CREATED"))
            .andExpect(jsonPath("$.data.totalPrice").value(34.50)).andExpect(jsonPath("$.data.items[0].discountedPrice").value(17.25));

        verify(orderService).createOrder(new CreateOrderCommand(42L, List.of(new CreateOrderItemCommand(701L, 2))));
    }

    @Test
    void 기본_배송지가_없어도_NO_ADDRESS_상태로_주문을_생성한다() throws Exception {
        authenticateAs(42L);
        when(orderService.createOrder(any())).thenReturn(result(false));

        mvc.perform(post("/api/v1/orders/checkout").contentType(MediaType.APPLICATION_JSON).content("""
                                {"items": [{"itemId": 701, "quantity": 2}]}
                                """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderKey").value("order_test")).andExpect(jsonPath("$.data.status").value("NO_ADDRESS"));
    }

    @Test
    void 주문_수량이_허용_범위를_벗어나면_서비스를_호출하지_않고_E400을_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/orders/checkout").contentType(MediaType.APPLICATION_JSON).content("""
                                {"items": [{"itemId": 701, "quantity": 501}]}
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void 주문상품_오류를_공통_ErrorResponse로_응답한다() throws Exception {
        authenticateAs(42L);
        when(orderService.createOrder(any())).thenThrow(new CoreException(ErrorCode.PRODUCT_MISMATCH_IN_ORDER));

        mvc.perform(post("/api/v1/orders/checkout").contentType(MediaType.APPLICATION_JSON).content("""
                                {"items": [{"itemId": 999, "quantity": 1}]}
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E3000")).andExpect(jsonPath("$.message").value("요청한 상품 정보와 일치하지 않습니다."));
    }

    @Test
    void 주문상품이_없으면_E400으로_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/orders/checkout").contentType(MediaType.APPLICATION_JSON).content("{\"items\": []}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void 주문키와_인증된_userId를_취소_Command로_변환하고_성공_외피를_반환한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(delete("/api/v1/orders/order_test/cancel")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(orderService).cancelOrder(new CancelOrderCommand(42L, "order_test"));
    }

    @Test
    void 주문키가_저장_길이를_초과하면_서비스를_호출하지_않는다() throws Exception {
        authenticateAs(42L);
        final String orderKey = "a".repeat(256);

        mvc.perform(delete("/api/v1/orders/" + orderKey + "/cancel")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verify(orderService, never()).cancelOrder(any());
    }

    @Test
    void 주문이_없으면_ORDER_NOT_FOUND와_404를_응답한다() throws Exception {
        authenticateAs(42L);
        doThrow(new CoreException(ErrorCode.ORDER_NOT_FOUND)).when(orderService).cancelOrder(any());

        mvc.perform(delete("/api/v1/orders/missing_order/cancel")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void 소유권이_없으면_E403을_응답한다() throws Exception {
        authenticateAs(42L);
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(orderService).cancelOrder(any());

        mvc.perform(delete("/api/v1/orders/another_users_order/cancel")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void 취소할_수_없는_상태면_ORDER_CANNOT_BE_CANCELED와_409를_응답한다() throws Exception {
        authenticateAs(42L);
        doThrow(new CoreException(ErrorCode.ORDER_CANNOT_BE_CANCELED)).when(orderService).cancelOrder(any());

        mvc.perform(delete("/api/v1/orders/paid_order/cancel")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value("ORDER_CANNOT_BE_CANCELED"));
    }

    private static CreateOrderResult result(final boolean canProceedToPayment) {
        return new CreateOrderResult("order_test", canProceedToPayment, new BigDecimal("34.50"), List.of(new CreateOrderItemResult(701L,
            "상품 701", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"), 2, new BigDecimal("34.50"))));
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
