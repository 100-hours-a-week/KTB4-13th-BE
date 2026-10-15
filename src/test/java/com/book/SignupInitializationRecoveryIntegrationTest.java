package com.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.application.service.CartService;
import com.book.core.cart.application.usecase.CreateCartUseCase;
import com.book.core.cart.domain.Cart;
import com.book.core.cart.infrastructure.scheduler.CartRecoveryScheduler;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.usecase.CreatePointBalanceUseCase;
import com.book.core.point.infrastructure.scheduler.PointBalanceRecoveryScheduler;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class SignupInitializationRecoveryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    CartRecoveryScheduler cartRecoveryScheduler;

    @Autowired
    PointBalanceRecoveryScheduler pointBalanceRecoveryScheduler;

    @Autowired
    CartService cartService;

    @Autowired
    UserRepositoryPort userRepository;

    @Autowired
    CreateCartUseCase createCartUseCase;

    @Autowired
    CreatePointBalanceUseCase createPointBalanceUseCase;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    CartRepositoryPort cartRepository;

    @MockitoSpyBean
    PointBalanceRepositoryPort pointBalanceRepository;

    @Test
    void 가입_10분이_지난_회원의_누락된_장바구니와_포인트_잔액을_반복_실행해도_한_번만_생성한다() {
        final Long userId = userRegisteredMinutesAgo(11);

        recoverAll();
        recoverAll();

        assertThat(cartCount(userId)).isEqualTo(1);
        assertThat(pointBalanceCount(userId)).isEqualTo(1);
    }

    @Test
    void 일부만_누락된_회원은_누락된_항목만_생성하고_기존_데이터를_유지한다() {
        final Long cartOnlyUserId = userRegisteredMinutesAgo(11);
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", cartOnlyUserId);
        final Long cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id = ?", Long.class, cartOnlyUserId);
        final Long pointOnlyUserId = userRegisteredMinutesAgo(11);
        jdbc.update("INSERT INTO point_balance (user_id, balance, created_at, updated_at) VALUES (?, 1500, NOW(6), NOW(6))",
            pointOnlyUserId);

        recoverAll();

        assertThat(jdbc.queryForObject("SELECT id FROM carts WHERE user_id = ?", Long.class, cartOnlyUserId)).isEqualTo(cartId);
        assertThat(pointBalanceCount(cartOnlyUserId)).isEqualTo(1);
        assertThat(cartCount(pointOnlyUserId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT balance FROM point_balance WHERE user_id = ?", Long.class, pointOnlyUserId))
            .isEqualTo(1500L);
    }

    @Test
    void 장바구니_복구가_실패해도_포인트_잔액은_복구하고_다음_실행에서_장바구니를_재시도한다() {
        final Long userId = userRegisteredMinutesAgo(11);
        doThrow(new DataAccessResourceFailureException("cart")).when(cartRepository).save(any(Cart.class));

        recoverAll();

        assertThat(cartCount(userId)).isZero();
        assertThat(pointBalanceCount(userId)).isEqualTo(1);

        reset(cartRepository);
        recoverAll();

        assertThat(cartCount(userId)).isEqualTo(1);
        assertThat(pointBalanceCount(userId)).isEqualTo(1);
    }

    @Test
    void 방금_가입한_회원과_탈퇴한_회원은_복구하지_않는다() {
        final Long recentUserId = userRepository.save(User.create("방금가입")).id();
        final Long oldUserId = userRegisteredMinutesAgo(11);
        final Long deletedUserId = userRegisteredMinutesAgo(11);
        jdbc.update("UPDATE users SET deleted_at = NOW(6) WHERE id = ?", deletedUserId);

        recoverAll();

        assertThat(cartCount(recentUserId)).isZero();
        assertThat(pointBalanceCount(recentUserId)).isZero();
        assertThat(cartCount(oldUserId)).isEqualTo(1);
        assertThat(pointBalanceCount(oldUserId)).isEqualTo(1);
        assertThat(cartCount(deletedUserId)).isZero();
        assertThat(pointBalanceCount(deletedUserId)).isZero();
    }

    @Test
    void 복구가_반복_실패하는_회원이_있어도_다음_페이지의_회원을_복구한다() {
        final Long failingUserId = userRegisteredMinutesAgo(11);
        final Long nextUserId = userRegisteredMinutesAgo(11);
        doThrow(new DataAccessResourceFailureException("cart")).when(cartRepository)
            .save(argThat((final Cart cart) -> cart.userId().equals(failingUserId)));

        cartService.recoverMissingCarts(LocalDateTime.now().minusMinutes(10), 1);
        cartService.recoverMissingCarts(LocalDateTime.now().minusMinutes(10), 1);

        assertThat(cartCount(failingUserId)).isZero();
        assertThat(cartCount(nextUserId)).isEqualTo(1);
    }

    @Test
    void 다른_세션이_스케줄러_잠금을_가지고_있으면_복구하지_않고_잠금_해제_후_다음_실행에서_복구한다() throws SQLException {
        final Long userId = userRegisteredMinutesAgo(11);
        try (Connection otherInstance = dataSource.getConnection()) {
            assertThat(lockQuery(otherInstance, "SELECT GET_LOCK('book_cart_recovery', 0)")).isEqualTo(1);
            assertThat(lockQuery(otherInstance, "SELECT GET_LOCK('book_point_balance_recovery', 0)")).isEqualTo(1);

            recoverAll();

            assertThat(cartCount(userId)).isZero();
            assertThat(pointBalanceCount(userId)).isZero();
            assertThat(lockQuery(otherInstance, "SELECT RELEASE_LOCK('book_cart_recovery')")).isEqualTo(1);
            assertThat(lockQuery(otherInstance, "SELECT RELEASE_LOCK('book_point_balance_recovery')")).isEqualTo(1);
        }

        recoverAll();

        assertThat(cartCount(userId)).isEqualTo(1);
        assertThat(pointBalanceCount(userId)).isEqualTo(1);
        try (Connection connection = dataSource.getConnection()) {
            assertThat(lockQuery(connection, "SELECT IS_FREE_LOCK('book_cart_recovery')")).isEqualTo(1);
            assertThat(lockQuery(connection, "SELECT IS_FREE_LOCK('book_point_balance_recovery')")).isEqualTo(1);
        }
    }

    @Test
    void 가입_초기화와_장바구니_복구가_겹치면_UNIQUE_제약으로_하나만_남고_복구_실패는_격리된다() throws Exception {
        final Long userId = userRegisteredMinutesAgo(11);

        raceWithRecoveryBetweenCheckAndInsert((final Answer<?> pause) -> doAnswer(pause).when(cartRepository).existsByUserId(userId),
            cartRecoveryScheduler::recover, () -> createCartUseCase.execute(userId));

        assertThat(cartCount(userId)).isEqualTo(1);
    }

    @Test
    void 포인트_잔액은_UNIQUE_제약이_없어_가입_초기화와_복구가_겹치면_중복_생성될_수_있다() throws Exception {
        final Long userId = userRegisteredMinutesAgo(11);

        raceWithRecoveryBetweenCheckAndInsert(
            (final Answer<?> pause) -> doAnswer(pause).when(pointBalanceRepository).existsByUserId(userId),
            pointBalanceRecoveryScheduler::recover, () -> createPointBalanceUseCase.execute(userId));

        assertThat(pointBalanceCount(userId)).isEqualTo(2);
        assertThatThrownBy(() -> pointBalanceRepository.findByUserId(userId)).isInstanceOf(IncorrectResultSizeDataAccessException.class);
    }

    // 복구가 기존 데이터 없음을 확인한 직후 멈춘 사이에 가입 초기화를 커밋하고, 이어서 복구가 생성하도록 순서를 고정합니다.
    private void raceWithRecoveryBetweenCheckAndInsert(final Consumer<Answer<?>> pauseAfterExistenceCheck, final Runnable recovery,
        final Runnable signupInitialization) throws Exception {
        final CountDownLatch checked = new CountDownLatch(1);
        final CountDownLatch signupCommitted = new CountDownLatch(1);
        pauseAfterExistenceCheck.accept((final InvocationOnMock invocation) -> {
            final Object exists = invocation.callRealMethod();
            checked.countDown();
            signupCommitted.await(10, TimeUnit.SECONDS);
            return exists;
        });
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<?> recoveryResult = executor.submit(recovery);
            assertThat(checked.await(10, TimeUnit.SECONDS)).isTrue();
            signupInitialization.run();
            signupCommitted.countDown();
            recoveryResult.get(10, TimeUnit.SECONDS);
        } finally {
            signupCommitted.countDown();
            executor.shutdownNow();
        }
    }

    private void recoverAll() {
        cartRecoveryScheduler.recover();
        pointBalanceRecoveryScheduler.recover();
    }

    // Hibernate가 JVM 기본 시간대로 created_at을 기록하는 것과 같은 기준으로 가입 시각을 저장합니다.
    private Long userRegisteredMinutesAgo(final long minutes) {
        final LocalDateTime registeredAt = LocalDateTime.now().minusMinutes(minutes);
        final GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update((final Connection connection) -> {
            final PreparedStatement statement = connection
                .prepareStatement("INSERT INTO users (nickname, created_at, updated_at) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "복구회원");
            statement.setObject(2, registeredAt);
            statement.setObject(3, registeredAt);
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private int lockQuery(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private Integer cartCount(final Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM carts WHERE user_id = ?", Integer.class, userId);
    }

    private Integer pointBalanceCount(final Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM point_balance WHERE user_id = ?", Integer.class, userId);
    }
}
