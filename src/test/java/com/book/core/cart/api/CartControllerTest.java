package com.book.core.cart.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.cart.api.converter.CartCommandConverter;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@Import(CartCommandConverter.class)
@ActiveProfiles("test")
class CartControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CartService cartService;

    @Test
    void quantity가_누락되면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":1,\"productId\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 상품_추가는_요청한_값으로_Command를_Service에_전달한다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":1,\"productId\":20,\"quantity\":3}"))
                .andExpect(status().isOk());

        verify(cartService).addCartItem(new AddCartItemCommand(42L, 20L, 3));
    }

    @Test
    void 잘못된_상품_ID는_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":1,\"productId\":0,\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 잘못된_수량은_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":1,\"productId\":20,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void userId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartId\":1,\"productId\":20,\"quantity\":1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }
}
