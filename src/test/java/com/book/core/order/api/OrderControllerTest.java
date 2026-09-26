package com.book.core.order.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.order.api.converter.OrderCommandConverter;
import com.book.core.order.api.converter.OrderResultConverter;
import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.order.application.result.CreateOrderItemResult;
import com.book.core.order.application.result.CreateOrderResult;
import com.book.core.order.application.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import({OrderCommandConverter.class, OrderResultConverter.class})
@ActiveProfiles("test")
class OrderControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    OrderService orderService;

    @Test
    void checkout_경로에서_임시_userId와_요청을_Command로_변환해_기존_응답_외피로_반환한다() throws Exception {
        when(orderService.createOrder(any())).thenReturn(result(true));

        mvc.perform(post("/api/v1/orders/checkout").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
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
        when(orderService.createOrder(any())).thenReturn(result(false));

        mvc.perform(post("/api/v1/orders/checkout").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
                                {"items": [{"itemId": 701, "quantity": 2}]}
                                """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderKey").value("order_test")).andExpect(jsonPath("$.data.status").value("NO_ADDRESS"));
    }

    @Test
    void 주문_수량이_허용_범위를_벗어나면_서비스를_호출하지_않고_E400을_응답한다() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
                                {"items": [{"itemId": 701, "quantity": 501}]}
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void 주문상품_오류를_공통_ErrorResponse로_응답한다() throws Exception {
        when(orderService.createOrder(any()))
            .thenThrow(new com.book.common.exception.CoreException(com.book.common.exception.ErrorCode.PRODUCT_MISMATCH_IN_ORDER));

        mvc.perform(post("/api/v1/orders/checkout").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("""
                                {"items": [{"itemId": 999, "quantity": 1}]}
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E3000")).andExpect(jsonPath("$.message").value("요청한 상품 정보와 일치하지 않습니다."));
    }

    @Test
    void 주문상품이_없으면_E400으로_응답한다() throws Exception {
        mvc.perform(
            post("/api/v1/orders/checkout").param("userId", "42").contentType(MediaType.APPLICATION_JSON).content("{\"items\": []}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    void 임시_userId가_없으면_서비스를_호출하지_않고_E400을_응답한다() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "items": [{"itemId": 701, "quantity": 1}]
                                }
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
    }

    private static CreateOrderResult result(final boolean canProceedToPayment) {
        return new CreateOrderResult("order_test", canProceedToPayment, new BigDecimal("34.50"), List.of(new CreateOrderItemResult(701L,
            "상품 701", null, "저자", new BigDecimal("20.00"), new BigDecimal("17.25"), 2, new BigDecimal("34.50"))));
    }
}
