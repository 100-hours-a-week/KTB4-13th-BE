package com.book.core.cart.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.api.converter.CartCommandConverter;
import com.book.core.cart.api.converter.CartResultConverter;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.service.CartService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@Import({CartCommandConverter.class, CartResultConverter.class})
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
                        .content("{\"productId\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 상품_추가는_요청한_값으로_Command를_Service에_전달한다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20,\"quantity\":3}"))
                .andExpect(status().isOk());

        verify(cartService).addCartItem(new AddCartItemCommand(42L, 20L, 3));
    }

    @Test
    void 잘못된_상품_ID는_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":0,\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 잘못된_수량은_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void userId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/cart/items")
                        .param("userId", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":20,\"quantity\":1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    void 장바구니_상품_삭제는_userId와_cartItemId를_Command로_전달하고_성공_응답한다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items/11").param("userId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cartService).deleteCartItem(new DeleteCartItemCommand(42L, 11L));
    }

    @Test
    void 삭제할_장바구니_상품이_없으면_404_오류_계약을_응답한다() throws Exception {
        doThrow(new CoreException(ErrorCode.CART_ITEM_NOT_FOUND))
                .when(cartService)
                .deleteCartItem(new DeleteCartItemCommand(42L, 11L));

        mvc.perform(delete("/api/v1/cart/items/11").param("userId", "42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    void 삭제_요청의_userId가_누락되면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items/11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 삭제_요청의_userId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items/11").param("userId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 삭제_요청의_cartItemId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items/0").param("userId", "42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 장바구니_상품_다건_삭제는_userId와_상품_ID_목록을_Command로_전달하고_성공_응답한다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartItemIds\":[11,12]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cartService).deleteCartItems(new DeleteCartItemsCommand(42L, List.of(11L, 12L)));
    }

    @Test
    void 다건_삭제_상품_ID_목록이_비어_있으면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartItemIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 다건_삭제_상품_ID가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartItemIds\":[11,0]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 다건_삭제_요청의_userId가_누락되면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartItemIds\":[11,12]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 다건_삭제할_장바구니_상품이_없으면_404_오류_계약을_응답한다() throws Exception {
        doThrow(new CoreException(ErrorCode.CART_ITEM_NOT_FOUND))
                .when(cartService)
                .deleteCartItems(new DeleteCartItemsCommand(42L, List.of(11L, 12L)));

        mvc.perform(delete("/api/v1/cart/items")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cartItemIds\":[11,12]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    void 장바구니_상품_수량_변경은_userId와_cartItemId와_수량을_Command로_전달한다() throws Exception {
        mvc.perform(put("/api/v1/cart/items/11")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(cartService).modifyCartItem(new ModifyCartItemCommand(42L, 11L, 4));
    }

    @Test
    void 수량이_0이면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(put("/api/v1/cart/items/11")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 수량이_500개를_초과하면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(put("/api/v1/cart/items/11")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":501}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void cartItemId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(put("/api/v1/cart/items/0")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(cartService);
    }

    @Test
    void 변경할_장바구니_상품이_없으면_404_오류_계약을_응답한다() throws Exception {
        doThrow(new CoreException(ErrorCode.CART_ITEM_NOT_FOUND))
                .when(cartService)
                .modifyCartItem(new ModifyCartItemCommand(42L, 11L, 4));

        mvc.perform(put("/api/v1/cart/items/11")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E401"));
    }
}
