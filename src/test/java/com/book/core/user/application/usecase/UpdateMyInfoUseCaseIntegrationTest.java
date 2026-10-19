package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.user.application.command.UpdateMyInfoCommand;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class UpdateMyInfoUseCaseIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    UpdateMyInfoUseCase updateMyInfoUseCase;

    @Autowired
    UserRepositoryPort userRepository;

    @Test
    void 수정_트랜잭션이_끝나면_변경_감지로_닉네임이_DB에_저장된다() {
        final Long userId = userRepository.save(User.create("북적이")).id();

        updateMyInfoUseCase.execute(new UpdateMyInfoCommand(userId, "  새로운북적이  "));

        assertThat(userRepository.findById(userId))
            .hasValueSatisfying((final User user) -> assertThat(user.nickname()).isEqualTo("새로운북적이"));
    }
}
