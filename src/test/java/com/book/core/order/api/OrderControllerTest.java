package com.book.core.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.book.core.order.application.command.GetOrderCommand;
import com.book.core.order.application.command.GetOrdersCommand;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.result.GetOrderResult;
import com.book.core.order.application.result.GetOrdersResult;
import com.book.core.order.application.result.OrderAddressResult;
import com.book.core.order.application.result.OrderItemResult;
import com.book.core.order.application.result.OrderSummaryResult;
import com.book.core.order.application.service.OrderService;
import com.book.core.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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
    void 인증된_회원의_주문_목록을_반환한다() throws Exception {
        authenticateAs(42L);
        when(orderService.getOrders(any())).thenReturn(GetOrdersResult.of(List.of(new OrderSummaryResult("order_new", "상품 701 외 1건",
            OrderStatus.PAID, new BigDecimal("34.50"), LocalDateTime.of(2026, 9, 28, 10, 0))), "next-cursor"));

        mvc.perform(get("/api/v1/orders").param("status", "PAID").param("from", "2026-01-01T00:00:00").param("to", "2026-09-28T23:59:59")
            .param("limit", "10")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orders[0].key").value("order_new"))
            .andExpect(jsonPath("$.data.orders[0].name").value("상품 701 외 1건")).andExpect(jsonPath("$.data.orders[0].status").value("PAID"))
            .andExpect(jsonPath("$.data.orders[0].totalPrice").value(34.50));

        verify(orderService).getOrders(new GetOrdersCommand(42L, OrderStatus.PAID, LocalDateTime.of(2026, 1, 1, 0, 0),
            LocalDateTime.of(2026, 9, 28, 23, 59, 59), null, 10));
    }

    @Test
    void 인증된_회원의_주문_상세와_상품을_반환한다() throws Exception {
        authenticateAs(42L);
        when(orderService.getOrder(any())).thenReturn(orderResult());

        mvc.perform(get("/api/v1/orders/order_test")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.key").value("order_test")).andExpect(jsonPath("$.data.name").value("상품 701"))
            .andExpect(jsonPath("$.data.status").value("CREATED")).andExpect(jsonPath("$.data.items[0].itemId").value(701))
            .andExpect(jsonPath("$.data.items[0].itemName").value("상품 701")).andExpect(jsonPath("$.data.items[0].quantity").value(2))
            .andExpect(jsonPath("$.data.address.postalCode").value("06236"));

        verify(orderService).getOrder(new GetOrderCommand(42L, "order_test"));
    }

    @Test
    void 다른_회원의_주문_상세는_E403을_응답한다() throws Exception {
        authenticateAs(42L);
        when(orderService.getOrder(any())).thenThrow(new CoreException(ErrorCode.FORBIDDEN));

        mvc.perform(get("/api/v1/orders/another_order")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void 선택한_배송지와_상품을_Command로_변환해_주문키를_반환한다() throws Exception {
        authenticateAs(42L);
        when(orderService.createOrder(any())).thenReturn(result(true));

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "addressId": 101,
                                  "items": [{"itemId": 701, "quantity": 2}]
                                }
                                """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderKey").value("order_test")).andExpect(jsonPath("$.data.status").doesNotExist());

        verify(orderService).createOrder(new CreateOrderCommand(42L, 101L, List.of(new CreateOrderItemCommand(701L, 2))));
    }

    @Test
    void 배송지_ID가_없으면_서비스를_호출하지_않고_E400을_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                                {"items": [{"itemId": 701, "quantity": 2}]}
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void 주문_수량이_허용_범위를_벗어나면_서비스를_호출하지_않고_E400을_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                                {"addressId": 101, "items": [{"itemId": 701, "quantity": 501}]}
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void 주문상품_오류를_공통_ErrorResponse로_응답한다() throws Exception {
        authenticateAs(42L);
        when(orderService.createOrder(any())).thenThrow(new CoreException(ErrorCode.PRODUCT_MISMATCH_IN_ORDER));

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                                {"addressId": 101, "items": [{"itemId": 999, "quantity": 1}]}
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E3000")).andExpect(jsonPath("$.message").value("요청한 상품 정보와 일치하지 않습니다."));
    }

    @Test
    void 주문상품이_없으면_E400으로_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{\"addressId\": 101, \"items\": []}"))
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
        return new CreateOrderResult("order_test", canProceedToPayment, new BigDecimal("34.50"), List.of());
    }

    private static GetOrderResult orderResult() {
        final OrderItemResult item = new OrderItemResult(17L, 701L, "상품 701", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"),
            new BigDecimal("34.50"), 2, com.book.core.order.domain.OrderItemStatus.CREATED);
        return new GetOrderResult("order_test", "상품 701", OrderStatus.CREATED, new BigDecimal("34.50"),
            LocalDateTime.of(2026, 9, 28, 10, 0), List.of(item), new OrderAddressResult("06236", "서울 주소", "101호"));
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
