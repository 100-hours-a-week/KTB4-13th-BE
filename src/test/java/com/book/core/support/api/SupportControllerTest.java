package com.book.core.support.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.support.config.WebCorsConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupportController.class)
@Import({SecurityConfiguration.class, JwtAuthenticationEntryPoint.class, WebCorsConfiguration.class})
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cors.allowed-origin=http://localhost:5173")
class SupportControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean(name = "serviceJwtDecoder")
    JwtDecoder serviceJwtDecoder;

    @Test
    void 상태_확인_API는_인증_없이_공통_성공_응답을_반환한다() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());
    }
}
