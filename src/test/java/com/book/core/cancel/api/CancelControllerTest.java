package com.book.core.cancel.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.api.config.SwaggerConfig;
import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.core.cancel.api.converter.CancelCommandConverter;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.service.CancelService;
import com.book.core.cancel.domain.CancelAction;
import com.book.support.config.WebCorsConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CancelController.class)
@Import({CancelCommandConverter.class, UserIdMvcConfig.class, SecurityConfiguration.class, JwtAuthenticationEntryPoint.class,
    WebCorsConfiguration.class, SwaggerConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class})
@ActiveProfiles("test")
class CancelControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private CancelService cancelService;
    @MockitoBean(name = "serviceJwtDecoder")
    private JwtDecoder jwtDecoder;

    @Test
    void 전체취소는_JWT회원과_주문키를_전달하고_참고처럼_빈_성공을_응답한다() throws Exception {
        authenticate();
        mvc.perform(post("/api/v1/cancel").header("Authorization", "Bearer test-token").contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderKey\":\"order\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());
        verify(cancelService).cancel(new CancelCommand(42L, new CancelAction("order")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"orderKey\":\" \"}", "{\"orderKey\":\"order\",\"orderItemId\":10,\"quantity\":0}",
        "{\"orderKey\":\"order\",\"orderItemId\":0,\"quantity\":1}", "{\"orderKey\":\"order\",\"orderItemId\":10,\"quantity\":-1}"})
    @Test
    void 소유권_불일치는_참고의_NOT_FOUND_DATA_계약으로_응답한다() throws Exception {
        authenticate();
        doThrow(new CoreException(ErrorCode.NOT_FOUND_DATA)).when(cancelService).cancel(new CancelCommand(42L, new CancelAction("other")));
        mvc.perform(post("/api/v1/cancel").header("Authorization", "Bearer test-token").contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderKey\":\"other\"}")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"));
    }

    private void authenticate() {
        when(jwtDecoder.decode("test-token")).thenReturn(Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build());
    }
}
