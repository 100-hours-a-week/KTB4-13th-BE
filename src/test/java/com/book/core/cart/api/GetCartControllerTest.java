package com.book.core.cart.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.cart.api.converter.CartCommandConverter;
import com.book.core.cart.api.converter.CartResultConverter;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.service.CartService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@Import({CartCommandConverter.class, CartResultConverter.class})
@ActiveProfiles("test")
class GetCartControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CartService cartService;

    @Test
    void userId로_장바구니를_조회한다() throws Exception {
        final GetCartCommand command = new GetCartCommand(42L);
        when(cartService.getCart(command)).thenReturn(new GetCartResult(List.of(new GetCartItemResult(11L, 200L, 2))));

        mvc.perform(get("/api/v1/cart").param("userId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].cartItemId").value(11))
                .andExpect(jsonPath("$.data.items[0].productId").value(200))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        verify(cartService).getCart(command);
    }

    @Test
    void 장바구니가_없으면_빈_items를_200으로_응답한다() throws Exception {
        final GetCartCommand command = new GetCartCommand(42L);
        when(cartService.getCart(command)).thenReturn(new GetCartResult(List.of()));

        mvc.perform(get("/api/v1/cart").param("userId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void userId가_누락되면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(get("/api/v1/cart"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void userId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(get("/api/v1/cart").param("userId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 조회_저장소_오류는_상세_원인없이_E500을_응답한다() throws Exception {
        when(cartService.getCart(any(GetCartCommand.class)))
                .thenThrow(new DataAccessResourceFailureException("database detail"));

        mvc.perform(get("/api/v1/cart").param("userId", "42"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }
}
