package com.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.usecase.AuthLoginUseCase;
import com.book.core.auth.infrastructure.client.kakao.KakaoOAuthProviderClientImpl;
import com.book.core.auth.infrastructure.token.JwtTokenIssuer;
import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.application.usecase.SampleCreateUseCase;
import com.book.core.sample.application.usecase.SampleQueryUseCase;
import com.book.core.user.application.port.NicknameGenerator;
import com.book.core.user.infrastructure.nickname.RandomNicknameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class BookApplicationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Autowired
    OAuthProviderClient oAuthProviderClient;

    @Autowired
    TokenIssuer tokenIssuer;

    @Autowired
    NicknameGenerator nicknameGenerator;

    @Autowired
    RefreshTokenHasher refreshTokenHasher;

    @Autowired
    RefreshSessionRepository refreshSessionRepository;

    @Autowired
    @Qualifier("kakaoJwtDecoder")
    JwtDecoder kakaoJwtDecoder;

    @Autowired
    @Qualifier("serviceJwtDecoder")
    JwtDecoder serviceJwtDecoder;

    @Autowired
    SecurityFilterChain securityFilterChain;

    @Test
    void 전체_Context에_각_UseCase와_Repository가_한_개씩_등록된다() {
        assertThat(context.getBeansOfType(AuthLoginUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(SampleCreateUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(SampleQueryUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(SampleRepository.class)).hasSize(1);
        assertThat(oAuthProviderClient).isInstanceOf(KakaoOAuthProviderClientImpl.class);
        assertThat(tokenIssuer).isInstanceOf(JwtTokenIssuer.class);
        assertThat(nicknameGenerator).isInstanceOf(RandomNicknameGenerator.class);
        assertThat(AopUtils.getTargetClass(refreshTokenHasher).getSimpleName()).isEqualTo("Sha256RefreshTokenHasher");
        assertThat(AopUtils.getTargetClass(refreshSessionRepository).getSimpleName())
                .isEqualTo("RefreshSessionRepositoryImpl");
        assertThat(kakaoJwtDecoder).isNotSameAs(serviceJwtDecoder);
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void HTTP_생성과_조회를_실제_MySQL까지_연결한다() throws Exception {
        final String authorization = "Bearer " + tokenIssuer.issue(1L).accessToken();
        final var created = mvc.perform(post("/api/v1/samples")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  통합 테스트  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("통합 테스트"))
                .andReturn()
                .getResponse();
        mvc.perform(get(created.getHeader("Location")).header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk())
                .andExpect(content().json(created.getContentAsString()));
        mvc.perform(get("/api/v1/samples/9223372036854775807").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAMPLE_NOT_FOUND"));
    }
}
