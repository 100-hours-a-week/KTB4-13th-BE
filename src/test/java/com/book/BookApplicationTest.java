package com.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.cart.application.port.CartRepository;
import com.book.core.cart.application.usecase.CartAddUseCase;
import com.book.core.sample.application.port.SampleRepository;
import com.book.core.sample.application.usecase.SampleCreateUseCase;
import com.book.core.sample.application.usecase.SampleQueryUseCase;
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
        assertThat(context.getBeansOfType(SampleCreateUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(SampleQueryUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(SampleRepository.class)).hasSize(1);
        assertThat(context.getBeansOfType(CartAddUseCase.class)).hasSize(1);
        assertThat(context.getBeansOfType(CartRepository.class)).hasSize(1);
    }

    @Test
    void HTTP_생성과_조회를_실제_MySQL까지_연결한다() throws Exception {
        final var created = mvc.perform(post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  통합 테스트  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("통합 테스트"))
                .andReturn()
                .getResponse();
        mvc.perform(get(created.getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(content().json(created.getContentAsString()));
        mvc.perform(get("/api/v1/samples/9223372036854775807"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E401"));
    }
}
