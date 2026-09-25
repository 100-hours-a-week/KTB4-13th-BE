package com.book.core.auth.infrastructure.token;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
import com.book.support.config.WebCorsConfiguration;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(SecurityIntegrationTest.ProtectedTestController.class)
@Import({
    SecurityConfiguration.class,
    JwtAuthenticationEntryPoint.class,
    JwtTokenConfiguration.class,
    JwtTokenIssuer.class,
    WebCorsConfiguration.class,
    SecurityIntegrationTest.ProtectedTestController.class
})
@TestPropertySource(
        properties = {
            "auth.token.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZg==",
            "auth.token.access-token-expiration=1h",
            "auth.token.refresh-token-expiration=7d",
            "app.cors.allowed-origin=http://localhost:5173"
        })
class SecurityIntegrationTest {
    private static final Long USER_ID = 42L;
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    MockMvc mvc;

    @Autowired
    TokenIssuer tokenIssuer;

    @Autowired
    TokenProperties properties;

    @Autowired
    @Qualifier(JwtTokenConfiguration.TOKEN_JWT_ENCODER)
    JwtEncoder jwtEncoder;

    @Test
    void Login_endpoint는_Access_Token_없이_접근할_수_있다() throws Exception {
        mvc.perform(post("/api/v1/auth/kakao/login")).andExpect(status().isOk());
    }

    @Test
    void 허용된_origin의_login_preflight에_credentials와_method와_header를_응답한다() throws Exception {
        mvc.perform(options("/api/v1/auth/kakao/login")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                HttpHeaders.AUTHORIZATION + ", " + HttpHeaders.CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")));
    }

    @Test
    void 허용된_origin의_login_POST에_CORS_header를_응답한다() throws Exception {
        mvc.perform(post("/api/v1/auth/kakao/login").header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void 허용되지_않은_origin의_preflight를_거부한다() throws Exception {
        mvc.perform(options("/api/v1/auth/kakao/login")
                        .header(HttpHeaders.ORIGIN, "http://malicious.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.CONTENT_TYPE))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void 유효한_Access_Token으로_authenticated_SecurityContext와_userId를_만든다() throws Exception {
        final String accessToken = tokenIssuer.issue(USER_ID).accessToken();

        mvc.perform(get("/test/protected").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.subject").value(USER_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void Refresh_Token을_Bearer로_사용하면_401을_응답한다() throws Exception {
        final String refreshToken = tokenIssuer.issue(USER_ID).refreshToken();

        assertUnauthorized(refreshToken);
    }

    @Test
    void 만료된_Access_Token은_401을_응답한다() throws Exception {
        final Instant expiredIssuedAt = Instant.now().minus(Duration.ofHours(2));
        final var expiredIssuer =
                new JwtTokenIssuer(jwtEncoder, properties, Clock.fixed(expiredIssuedAt, ZoneOffset.UTC));

        assertUnauthorized(expiredIssuer.issue(USER_ID).accessToken());
    }

    @Test
    void 잘못된_signature의_Access_Token은_401을_응답한다() throws Exception {
        final byte[] otherSecretBytes = new byte[64];
        Arrays.fill(otherSecretBytes, (byte) 1);
        final SecretKey otherSecretKey = new SecretKeySpec(otherSecretBytes, "HmacSHA512");
        final JwtEncoder otherEncoder = NimbusJwtEncoder.withSecretKey(otherSecretKey)
                .algorithm(MacAlgorithm.HS512)
                .build();
        final var otherIssuer = new JwtTokenIssuer(otherEncoder, properties, Clock.systemUTC());

        assertUnauthorized(otherIssuer.issue(USER_ID).accessToken());
    }

    @Test
    void Token_없이_보호된_endpoint를_요청하면_401을_응답한다() throws Exception {
        mvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E401"))
                .andExpect(jsonPath("$.message").value("인증 정보가 유효하지 않습니다."))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private void assertUnauthorized(final String token) throws Exception {
        mvc.perform(get("/test/protected").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E401"))
                .andExpect(jsonPath("$.message").value("인증 정보가 유효하지 않습니다."))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private String bearer(final String token) {
        return "Bearer " + token;
    }

    @RestController
    public static class ProtectedTestController {
        @PostMapping("/api/v1/auth/{providerType}/login")
        Map<String, String> loginEndpoint() {
            return Map.of("result", "SUCCESS");
        }

        @GetMapping("/test/protected")
        Map<String, Object> protectedEndpoint(final Authentication authentication) {
            final Jwt principal = (Jwt) authentication.getPrincipal();
            final Long userId = Long.valueOf(principal.getSubject());
            return Map.of(
                    "authenticated", authentication.isAuthenticated(),
                    "subject", principal.getSubject(),
                    "userId", userId);
        }
    }
}
