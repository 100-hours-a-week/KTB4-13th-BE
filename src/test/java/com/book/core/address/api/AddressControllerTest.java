package com.book.core.address.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.service.AddressService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AddressController.class)
@Import({AddressCommandConverter.class, AddressResultConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class AddressControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AddressService addressService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 주소지를_등록하면_성공_응답을_반환한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/user-addresses").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "detailAddress": "101호",
                                  "isDefault": false
                                }
                                """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(addressService).registerAddress(new RegisterAddressCommand(42L, "집", "12345", "서울시 강남구", "101호", false));
    }

    @Test
    void isDefault가_누락되면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/user-addresses").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구"
                                }
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void isDefault가_false면_false로_Command를_전달한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/user-addresses").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "isDefault": false
                                }
                                """)).andExpect(status().isOk());

        verify(addressService).registerAddress(new RegisterAddressCommand(42L, "집", "12345", "서울시 강남구", null, false));
    }

    @Test
    void 필수_주소지_값이_공백이면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/user-addresses").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                  "label": "   ",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "isDefault": false
                                }
                                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"label", "postalCode", "address", "detailAddress"})
    void 등록_요청에서_255자를_초과하면_400을_응답하고_Service를_호출하지_않는다(final String field) throws Exception {
        authenticateAs(42L);
        final var fields = new HashMap<String, Object>(Map.of(
            "label", "집", "postalCode", "12345", "address", "서울시 강남구", "detailAddress", "101호", "isDefault", false));
        fields.put(field, "가".repeat(256));
        final String body = JsonMapper.builder().build().writeValueAsString(fields);

        mvc.perform(post("/api/v1/user-addresses").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void 등록_요청에서_255자까지_허용한다() throws Exception {
        authenticateAs(42L);
        final String maxLengthValue = "가".repeat(255);
        final var command = new RegisterAddressCommand(42L, maxLengthValue, maxLengthValue, maxLengthValue, maxLengthValue, false);
        final String body = JsonMapper.builder().build().writeValueAsString(Map.of(
            "label", maxLengthValue, "postalCode", maxLengthValue, "address", maxLengthValue, "detailAddress", maxLengthValue, "isDefault",
            false));

        mvc.perform(post("/api/v1/user-addresses").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());

        verify(addressService).registerAddress(command);
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
