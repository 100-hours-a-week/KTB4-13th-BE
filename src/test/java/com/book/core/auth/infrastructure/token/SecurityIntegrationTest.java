package com.book.core.auth.infrastructure.token;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.book.core.auth.infrastructure.security.SecurityConfiguration;
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
    SecurityIntegrationTest.ProtectedTestController.class
})
@TestPropertySource(
        properties = {
            "auth.token.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWYwMTIzNDU2Nzg5YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZg==",
            "auth.token.access-token-expiration=1h",
            "auth.token.refresh-token-expiration=7d"
        })
class SecurityIntegrationTest {
    private static final Long USER_ID = 42L;

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
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    private void assertUnauthorized(final String token) throws Exception {
        mvc.perform(get("/test/protected").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"))
                .andExpect(jsonPath("$.message").value("Access Token이 유효하지 않습니다."));
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
