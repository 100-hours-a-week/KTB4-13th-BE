package com.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.usecase.GetAddressesUseCase;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
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

    @Test
    void 전체_Context에_각_UseCase와_Repository가_한_개씩_등록된다() {
        assertThat(context.getBeansOfType(RegisterAddressUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(GetAddressesUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(AddressRepositoryPort.class)).hasSize(1);
        assertThat(context.getBeansOfType(AddCartItemUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(CartRepositoryPort.class)).hasSize(1);
    }

    @Test
    void HTTP_주소지_등록을_실제_MySQL까지_연결한다() throws Exception {
        mvc.perform(post("/api/v1/user-addresses")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "detailAddress": "101호",
                                  "isDefault": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
