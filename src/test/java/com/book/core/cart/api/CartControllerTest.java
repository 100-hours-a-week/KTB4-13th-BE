package com.book.core.cart.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.usecase.CartAddUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@ActiveProfiles("test")
class CartControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CartAddUseCase addUseCase;

    @Test
    void 상품_추가는_수량을_생략하면_1로_Command를_호출한다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        final var command = org.mockito.ArgumentCaptor.forClass(CartAddCommand.class);
        verify(addUseCase).execute(command.capture());
        assertThat(command.getValue()).isEqualTo(new CartAddCommand(42L, 20L, 1));
    }

    @Test
    void 상품_추가는_요청한_수량으로_Command를_호출한다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20,\"quantity\":3}"))
                .andExpect(status().isOk());

        verify(addUseCase).execute(new CartAddCommand(42L, 20L, 3));
    }

    @Test
    void 잘못된_상품_ID는_400을_응답하고_UseCase를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addUseCase);
    }

    @Test
    void 잘못된_수량은_400을_응답하고_UseCase를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addUseCase);
    }

    @Test
    void userId가_양수가_아니면_400을_응답하고_UseCase를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(addUseCase);
    }
}
