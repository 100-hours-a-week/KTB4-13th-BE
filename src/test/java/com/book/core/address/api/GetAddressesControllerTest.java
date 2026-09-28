package com.book.core.address.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.result.GetAddressesResult;
import com.book.core.address.application.service.AddressService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(AddressController.class)
@Import({AddressCommandConverter.class, AddressResultConverter.class, GetAddressesControllerTest.AuthenticationPrincipalTestConfig.class})
@ActiveProfiles("test")
class GetAddressesControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AddressService addressService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증_회원의_주소지_목록을_명세_필드명으로_응답한다() throws Exception {
        authenticateAs(42L);
        final GetAddressesCommand command = new GetAddressesCommand(42L);
        when(addressService.getAddresses(command))
            .thenReturn(new GetAddressesResult(List.of(new GetAddressItemResult(101L, "집", "06236", "서울특별시 강남구 테헤란로 1", "101호", true))));

        mvc.perform(get("/api/v1/user-addresses")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.addresses[0].addressid").value(101))
            .andExpect(jsonPath("$.data.addresses[0].addressId").doesNotExist())
            .andExpect(jsonPath("$.data.addresses[0].addressLabel").value("집"))
            .andExpect(jsonPath("$.data.addresses[0].addressPostalCode").value("06236"))
            .andExpect(jsonPath("$.data.addresses[0].address").value("서울특별시 강남구 테헤란로 1"))
            .andExpect(jsonPath("$.data.addresses[0].detailAddress").value("101호"))
            .andExpect(jsonPath("$.data.addresses[0].isDefault").value(true)).andExpect(jsonPath("$.data.nextCursor").isEmpty());

        verify(addressService).getAddresses(command);
    }

    @Test
    void 저장소_오류는_E500으로_응답한다() throws Exception {
        authenticateAs(42L);
        when(addressService.getAddresses(any(GetAddressesCommand.class))).thenThrow(new IllegalStateException("database detail"));

        mvc.perform(get("/api/v1/user-addresses")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
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
