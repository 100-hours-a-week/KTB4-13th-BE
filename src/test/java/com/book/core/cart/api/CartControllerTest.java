package com.book.core.cart.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.cart.application.command.CartAddCommand;
import com.book.core.cart.application.command.CartChangeQuantityCommand;
import com.book.core.cart.application.command.CartDeleteCommand;
import com.book.core.cart.application.result.CartItemResult;
import com.book.core.cart.application.result.CartQueryResult;
import com.book.core.cart.application.usecase.CartAddUseCase;
import com.book.core.cart.application.usecase.CartChangeQuantityUseCase;
import com.book.core.cart.application.usecase.CartDeleteUseCase;
import com.book.core.cart.application.usecase.CartQueryUseCase;
import java.util.List;
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
    CartQueryUseCase queryUseCase;

    @MockitoBean
    CartAddUseCase addUseCase;

    @MockitoBean
    CartChangeQuantityUseCase changeQuantityUseCase;

    @MockitoBean
    CartDeleteUseCase deleteUseCase;

    @Test
    void userId_파라미터로_장바구니를_조회한다() throws Exception {
        when(queryUseCase.execute(42L)).thenReturn(new CartQueryResult(List.of(new CartItemResult(9L, 20L, 2))));

        mvc.perform(get("/api/v1/cart").param("userId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cartItems[0].cartItemId").value(9))
                .andExpect(jsonPath("$.data.cartItems[0].quantity").value(2));
    }

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
    void 수량변경과_단건_다건삭제_경로를_각각_호출한다() throws Exception {
        mvc.perform(put("/api/v1/cart/items/9")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(delete("/api/v1/cart/items/9").param("userId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mvc.perform(delete("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartItemIds\":[10,11,10]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(changeQuantityUseCase).execute(new CartChangeQuantityCommand(42L, 9L, 3));
        verify(deleteUseCase).execute(new CartDeleteCommand(42L, List.of(9L)));
        verify(deleteUseCase).execute(new CartDeleteCommand(42L, List.of(10L, 11L)));
    }

    @Test
    void 상품_ID가_양수가_아니면_400을_응답하고_UseCase를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(addUseCase);
    }

    @Test
    void 상품_추가_수량이_0이면_400을_응답하고_UseCase를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(addUseCase);
    }

    @Test
    void 수량_변경은_1보다_작은_값을_도메인에_위임한다() throws Exception {
        mvc.perform(put("/api/v1/cart/items/9")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(changeQuantityUseCase).execute(new CartChangeQuantityCommand(42L, 9L, 0));
    }

    @Test
    void userId_파라미터가_없으면_400을_응답한다() throws Exception {
        mvc.perform(get("/api/v1/cart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(queryUseCase);
    }

    @Test
    void userId가_양수가_아니면_400을_응답한다() throws Exception {
        mvc.perform(get("/api/v1/cart").param("userId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(queryUseCase);
    }

    @Test
    void cartItemId가_양수가_아니면_400을_응답하고_UseCase를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items/0").param("userId", "42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(deleteUseCase);
    }
}
