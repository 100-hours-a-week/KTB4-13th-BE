package com.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.usecase.GetAddressesUseCase;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.port.RefreshTokenVerifier;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.usecase.AuthLoginUseCase;
import com.book.core.auth.application.usecase.AuthLogoutUseCase;
import com.book.core.auth.application.usecase.AuthReissueUseCase;
import com.book.core.auth.infrastructure.client.kakao.KakaoOAuthProviderClientImpl;
import com.book.core.auth.infrastructure.client.kakao.KakaoOAuthTokenClientImpl;
import com.book.core.auth.infrastructure.token.JwtRefreshTokenVerifier;
import com.book.core.auth.infrastructure.token.JwtTokenIssuer;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.user.application.port.NicknameGenerator;
import com.book.core.user.infrastructure.nickname.RandomNicknameGenerator;
import org.junit.jupiter.api.Tag;
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
@Tag("integration")
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
    OAuthTokenClient oAuthTokenClient;

    @Autowired
    TokenIssuer tokenIssuer;

    @Autowired
    RefreshTokenHasher refreshTokenHasher;

    @Autowired
    RefreshTokenVerifier refreshTokenVerifier;

    @Autowired
    RefreshSessionRepository refreshSessionRepository;

    @Autowired
    NicknameGenerator nicknameGenerator;

    @Autowired
    @Qualifier("kakaoJwtDecoder")
    JwtDecoder kakaoJwtDecoder;

    @Autowired
    @Qualifier("serviceJwtDecoder")
    JwtDecoder serviceJwtDecoder;

    @Autowired
    SecurityFilterChain securityFilterChain;

    @Autowired
    AddressRepositoryPort addressRepository;

    @Test
    void 전체_Context에_각_UseCase와_Repository가_한_개씩_등록된다() {
        assertThat(context.getBeansOfType(RegisterAddressUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(GetAddressesUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(AddressRepositoryPort.class)).hasSize(1);
        assertThat(context.getBeansOfType(AuthLoginUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(AuthReissueUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(AuthLogoutUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(AddCartItemUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(CartRepositoryPort.class)).hasSize(1);
        assertThat(oAuthProviderClient).isInstanceOf(KakaoOAuthProviderClientImpl.class);
        assertThat(oAuthTokenClient).isInstanceOf(KakaoOAuthTokenClientImpl.class);
        assertThat(tokenIssuer).isInstanceOf(JwtTokenIssuer.class);
        assertThat(refreshTokenVerifier).isInstanceOf(JwtRefreshTokenVerifier.class);
        assertThat(AopUtils.getTargetClass(refreshTokenHasher).getSimpleName()).isEqualTo("Sha256RefreshTokenHasher");
        assertThat(AopUtils.getTargetClass(refreshSessionRepository).getSimpleName()).isEqualTo("RefreshSessionRepositoryAdapter");
        assertThat(nicknameGenerator).isInstanceOf(RandomNicknameGenerator.class);
        assertThat(kakaoJwtDecoder).isNotSameAs(serviceJwtDecoder);
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void HTTP_주소지_등록을_실제_MySQL까지_연결한다() throws Exception {
        final String authorization = "Bearer " + tokenIssuer.issue(42L).accessToken();
        final var request =
            post("/api/v1/user-addresses").header(HttpHeaders.AUTHORIZATION, authorization).contentType(MediaType.APPLICATION_JSON);
        mvc.perform(request.content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "detailAddress": "101호",
                                  "isDefault": true
                                }
                                """)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").doesNotExist());

        assertThat(addressRepository.countActiveByUserId(42L)).isEqualTo(1);
    }

    @Test
    void 인증_없이_온보딩_도서_후보를_조회하면_401을_응답한다() throws Exception {
        mvc.perform(get("/api/v1/onboarding/books")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("E401"));
    }

    @Test
    void actuatorHealthIsUnauthenticatedAndReturnsOnlyUpAndHidesInfo() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());

        final String authorization = "Bearer " + tokenIssuer.issue(42L).accessToken();
        mvc.perform(get("/actuator/info").header(HttpHeaders.AUTHORIZATION, authorization)).andExpect(status().isNotFound());
    }
}
