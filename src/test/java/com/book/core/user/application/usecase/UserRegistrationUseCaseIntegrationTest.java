package com.book.core.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.domain.ProviderType;
import java.math.BigDecimal;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class UserRegistrationUseCaseIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    UserRegistrationUseCase userRegistrationUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    CartRepositoryPort cartRepository;

    @MockitoSpyBean
    PointBalanceRepositoryPort pointBalanceRepository;

    @Test
    void 회원가입은_새_user의_Cart와_잔액_0인_포인트를_하나씩_저장한다() {
        final UserRegistrationCommand command = new UserRegistrationCommand(ProviderType.KAKAO, "cart-success-provider", null);

        final Long userId = userRegistrationUseCase.execute(command);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_providers WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carts WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_balance WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = ?", BigDecimal.class, userId))
            .isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_history WHERE user_id = ?", Integer.class, userId)).isZero();
    }

    @Test
    void cart_저장_실패는_User와_UserProvider를_함께_rollback한다() {
        final UserRegistrationCommand command = new UserRegistrationCommand(ProviderType.KAKAO, "cart-save-failure-provider", null);
        final Integer usersBefore = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        doThrow(new DataAccessResourceFailureException("cart save failed")).when(cartRepository).save(any(Cart.class));

        assertThatThrownBy(() -> userRegistrationUseCase.execute(command)).isInstanceOf(DataAccessResourceFailureException.class);

        verify(cartRepository).save(any(Cart.class));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(usersBefore);
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM user_providers WHERE provider_user_id = ?", Integer.class, command.providerUserId()))
            .isZero();
    }

    @Test
    void 포인트_저장_실패는_회원과_Provider와_Cart를_함께_rollback한다() {
        final UserRegistrationCommand command = new UserRegistrationCommand(ProviderType.KAKAO, "point-save-failure-provider", null);
        doThrow(new DataAccessResourceFailureException("point save failed")).when(pointBalanceRepository).save(any(PointBalance.class));

        assertThatThrownBy(() -> userRegistrationUseCase.execute(command)).isInstanceOf(DataAccessResourceFailureException.class);

        final ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(captor.capture());
        final Long userId = captor.getValue().userId();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_providers WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM carts WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM point_balance WHERE user_id = ?", Integer.class, userId)).isZero();
    }
}
