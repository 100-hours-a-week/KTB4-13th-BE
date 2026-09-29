package com.book.core.cart.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.cart.api.converter.CartCommandConverter;
import com.book.core.cart.api.converter.CartResultConverter;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.service.CartService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@Import({CartCommandConverter.class, CartResultConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class GetCartControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CartService cartService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_userId로_장바구니를_조회한다() throws Exception {
        authenticateAs(42L);
        final GetCartCommand command = new GetCartCommand(42L);
        when(cartService.getCart(command)).thenReturn(new GetCartResult(List.of(new GetCartItemResult(11L, 200L, 2))));

        mvc.perform(get("/api/v1/cart")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].cartItemId").value(11)).andExpect(jsonPath("$.data.items[0].productId").value(200))
            .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        verify(cartService).getCart(command);
    }

    @Test
    void 장바구니가_없으면_빈_items를_200으로_응답한다() throws Exception {
        authenticateAs(42L);
        final GetCartCommand command = new GetCartCommand(42L);
        when(cartService.getCart(command)).thenReturn(new GetCartResult(List.of()));

        mvc.perform(get("/api/v1/cart")).andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 조회_저장소_오류는_상세_원인없이_E500을_응답한다() throws Exception {
        authenticateAs(42L);
        when(cartService.getCart(any(GetCartCommand.class))).thenThrow(new DataAccessResourceFailureException("database detail"));

        mvc.perform(get("/api/v1/cart")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
