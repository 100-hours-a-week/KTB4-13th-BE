package com.book.core.agentchat.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.PrepareCartChangeCommand;
import com.book.core.agentchat.application.port.CartChangeRepositoryPort;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.application.usecase.ExecuteCartChangeUseCase;
import com.book.core.agentchat.application.usecase.GetCartExecutionUseCase;
import com.book.core.agentchat.application.usecase.GetCartProposalUseCase;
import com.book.core.agentchat.application.usecase.PrepareCartChangeUseCase;
import com.book.core.agentchat.domain.CartChangeLine;
import com.book.core.agentchat.domain.CartChangeOperation;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.result.GetCartItemResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class CartChangeIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-18T00:00:00Z");
    private static final BigDecimal UNIT_PRICE = new BigDecimal("18000.00");
    private static final AtomicLong NEXT_USER_ID = new AtomicLong(710000L);

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    private PrepareCartChangeUseCase prepare;
    @Autowired
    private ExecuteCartChangeUseCase execute;
    @Autowired
    private GetCartExecutionUseCase getExecution;
    @Autowired
    private GetCartProposalUseCase getProposal;
    @Autowired
    private GetCartUseCase getCart;
    @Autowired
    private AddCartItemUseCase add;
    @Autowired
    private ModifyCartItemUseCase modify;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private CartChangeRepositoryPort changes;
    @MockitoBean
    private Clock clock;

    private long userId;
    private long firstProductId;
    private long secondProductId;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        userId = NEXT_USER_ID.getAndAdd(1000);
        firstProductId = userId + 1;
        secondProductId = userId + 2;
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", userId);
        createProduct(firstProductId, 100);
        createProduct(secondProductId, 100);
    }

    @Test
    void 잘못된_수량은_제안과_요청을_저장하기_전에_거절한다() {
        final CartChangeLine invalid = new CartChangeLine(CartChangeOperation.ADD, firstProductId, 0, 501, UNIT_PRICE);
        assertThatThrownBy(() -> prepare.createProposal(command("invalid-proposal", List.of(invalid)))).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> prepare.prepareDirect(command("invalid-request", List.of(invalid)))).isInstanceOf(CoreException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_proposals WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_change_requests WHERE user_id = ?", Integer.class, userId)).isZero();
        assertThat(getCart.execute(userId).items()).isEmpty();
    }

    @Test
    void 여러_상품의_추가_수량대체_삭제와_성공기록을_함께_확정한다() {
        add.execute(new AddCartItemCommand(userId, firstProductId, 3));
        add.execute(new AddCartItemCommand(userId, secondProductId, 1));
        final long thirdProductId = userId + 3;
        createProduct(thirdProductId, 100);
        final Long requestId = prepare.prepareDirect(command("mixed",
            List.of(line(CartChangeOperation.ADD, firstProductId, 3, 2),
                new CartChangeLine(CartChangeOperation.REMOVE, secondProductId, 1, null, null),
                line(CartChangeOperation.ADD, thirdProductId, 0, 4))));

        final CartChangeResult result = execute.execute(userId, requestId);

        assertThat(result.rejection()).isNull();
        assertThat(result.executionId()).isNotNull();
        assertThat(result.proposalId()).isNull();
        assertThat(result.appliedAt()).isEqualTo(NOW);
        assertThat(result.replayed()).isFalse();
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::productId, GetCartItemResult::quantity)
            .containsExactlyInAnyOrder(tuple(firstProductId, 2), tuple(thirdProductId, 4));
        final CartChangeResult saved = getExecution.execute(userId, result.executionId());
        assertThat(saved.items()).usingRecursiveComparison().isEqualTo(result.items());
        assertThat(saved.appliedAt()).isEqualTo(NOW);
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 한_상품만_품절이어도_전체_미적용하고_같은_요청의_거절을_재생한다() {
        jdbc.update("UPDATE products SET stock_quantity = 0 WHERE id = ?", secondProductId);
        final Long requestId = prepare.prepareDirect(command("out-of-stock",
            List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2), line(CartChangeOperation.ADD, secondProductId, 0, 1))));

        final CartChangeResult rejected = execute.execute(userId, requestId);
        jdbc.update("UPDATE products SET stock_quantity = 100 WHERE id = ?", secondProductId);
        final CartChangeResult replay = execute.execute(userId, requestId);

        assertThat(rejected.rejection()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        assertThat(rejected.executionId()).isNull();
        assertThat(replay.rejection()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK);
        assertThat(replay.replayed()).isTrue();
        assertThat(getCart.execute(userId).items()).isEmpty();
        assertThat(executionCount()).isZero();
    }

    @Test
    void 기준_수량이_바뀌면_직접_지시도_전체_거절한다() {
        add.execute(new AddCartItemCommand(userId, firstProductId, 3));
        final Long requestId = prepare.prepareDirect(command("quantity",
            List.of(line(CartChangeOperation.UPDATE, firstProductId, 3, 2), line(CartChangeOperation.ADD, secondProductId, 0, 1))));
        modify.execute(new ModifyCartItemCommand(userId, cartItemId(firstProductId), 5));

        final CartChangeResult result = execute.execute(userId, requestId);

        assertThat(result.rejection()).isEqualTo(ErrorCode.CART_STATE_CHANGED);
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::productId, GetCartItemResult::quantity)
            .containsExactly(tuple(firstProductId, 5));
        assertThat(executionCount()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"17000.00", "19000.00"})
    void 기준_단가의_인하와_인상은_모두_전체_거절한다(final String changedPrice) {
        final Long requestId = prepare.prepareDirect(command("price",
            List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2), line(CartChangeOperation.ADD, secondProductId, 0, 1))));
        jdbc.update("UPDATE products SET discounted_price = ? WHERE id = ?", new BigDecimal(changedPrice), secondProductId);

        final CartChangeResult result = execute.execute(userId, requestId);

        assertThat(result.rejection()).isEqualTo(ErrorCode.CART_PRICE_CHANGED);
        assertThat(getCart.execute(userId).items()).isEmpty();
        assertThat(executionCount()).isZero();
    }

    @Test
    void 같은_키의_같은_의도는_새_관측값으로_첫_기준을_덮어쓰지_않는다() {
        final Long requestId = prepare.prepareDirect(command("first-basis", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        add.execute(new AddCartItemCommand(userId, firstProductId, 5));

        final Long retryRequestId =
            prepare.prepareDirect(command("first-basis", List.of(line(CartChangeOperation.ADD, firstProductId, 5, 2))));
        final CartChangeResult result = execute.execute(userId, retryRequestId);

        assertThat(retryRequestId).isEqualTo(requestId);
        assertThat(result.rejection()).isEqualTo(ErrorCode.CART_STATE_CHANGED);
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::quantity).containsExactly(5);
        assertThat(executionCount()).isZero();
    }

    @Test
    void 같은_키에_다른_수량을_넣으면_성공_후에도_충돌로_거절한다() {
        final Long requestId =
            prepare.prepareDirect(command("different-intent", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        execute.execute(userId, requestId);

        assertThatThrownBy(
            () -> prepare.prepareDirect(command("different-intent", List.of(line(CartChangeOperation.ADD, firstProductId, 2, 3)))))
            .isInstanceOfSatisfying(CoreException.class,
                (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::quantity).containsExactly(2);
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 같은_키의_동시_직접_요청도_성공_실행은_한번만_만든다() throws Exception {
        final PrepareCartChangeCommand command = command("concurrent", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2)));
        final List<CartChangeResult> results = concurrently(() -> execute.execute(userId, prepare.prepareDirect(command)),
            () -> execute.execute(userId, prepare.prepareDirect(command)));

        assertThat(results).allSatisfy((final CartChangeResult result) -> assertThat(result.rejection()).isNull());
        assertThat(results).extracting(CartChangeResult::executionId).containsOnly(results.getFirst().executionId());
        assertThat(results).isNotEmpty();
        assertThat(execute.execute(userId, prepare.prepareDirect(command)).replayed()).isTrue();
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::quantity).containsExactly(2);
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 다른_키로_같은_제안을_동시_확인해도_같은_성공을_반환한다() throws Exception {
        final Long proposalId = prepare.createProposal(command("proposal", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        assertThat(getCart.execute(userId).items()).isEmpty();

        final List<CartChangeResult> results =
            concurrently(() -> execute.confirm(userId, proposalId, "confirm-a"), () -> execute.confirm(userId, proposalId, "confirm-b"));

        assertThat(results).allSatisfy((final CartChangeResult result) -> assertThat(result.rejection()).isNull());
        assertThat(results).extracting(CartChangeResult::executionId).containsOnly(results.getFirst().executionId());
        assertThat(getProposal.execute(userId, proposalId).status()).isEqualTo("EXECUTED");
        assertThat(getProposal.execute(userId, proposalId).executionId()).isEqualTo(results.getFirst().executionId());
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 응답_유실_후_재전송은_일반_화면의_후속_변경을_보존하고_과거_성공을_반환한다() {
        add.execute(new AddCartItemCommand(userId, firstProductId, 3));
        final Long requestId =
            prepare.prepareDirect(command("lost-response", List.of(line(CartChangeOperation.UPDATE, firstProductId, 3, 2))));
        final CartChangeResult applied = execute.execute(userId, requestId);
        modify.execute(new ModifyCartItemCommand(userId, cartItemId(firstProductId), 5));
        when(clock.instant()).thenReturn(NOW.plusSeconds(3600));

        final CartChangeResult replay = execute.execute(userId, requestId);
        final CartChangeResult saved = getExecution.execute(userId, applied.executionId());

        assertThat(replay.executionId()).isEqualTo(applied.executionId());
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.items()).usingRecursiveComparison().isEqualTo(applied.items());
        assertThat(saved.appliedAt()).isEqualTo(NOW);
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::quantity).containsExactly(5);
        assertThat(executionCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"599999, true", "600000, false", "600001, false"})
    void 제안은_10분_직전까지만_새로_실행할_수_있다(final long elapsedMillis, final boolean executable) {
        final Long proposalId = prepare.createProposal(command("expires", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        assertThat(getProposal.execute(userId, proposalId).expiresAt()).isEqualTo(Instant.parse("2026-10-18T00:10:00Z"));
        when(clock.instant()).thenReturn(NOW.plusMillis(elapsedMillis));

        final CartChangeResult result = execute.confirm(userId, proposalId, "confirm-expiry");

        if (executable) {
            assertThat(result.rejection()).isNull();
            assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::quantity).containsExactly(2);
            assertThat(executionCount()).isEqualTo(1);
            return;
        }
        assertThat(result.rejection()).isEqualTo(ErrorCode.PROPOSAL_NOT_EXECUTABLE);
        assertThat(getProposal.execute(userId, proposalId).status()).isEqualTo("EXPIRED");
        assertThat(getCart.execute(userId).items()).isEmpty();
        assertThat(executionCount()).isZero();
    }

    @Test
    void 이미_성공한_제안은_10분_후_동일키와_다른키_모두_과거_성공을_반환한다() {
        final Long proposalId =
            prepare.createProposal(command("expired-success", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        final CartChangeResult applied = execute.confirm(userId, proposalId, "confirmed");
        modify.execute(new ModifyCartItemCommand(userId, cartItemId(firstProductId), 5));
        when(clock.instant()).thenReturn(NOW.plusSeconds(86400));

        final CartChangeResult sameKey = execute.confirm(userId, proposalId, "confirmed");
        final CartChangeResult otherKey = execute.confirm(userId, proposalId, "another-key");

        assertThat(sameKey.executionId()).isEqualTo(applied.executionId());
        assertThat(otherKey.executionId()).isEqualTo(applied.executionId());
        assertThat(sameKey.replayed()).isTrue();
        assertThat(otherKey.replayed()).isTrue();
        assertThat(getProposal.execute(userId, proposalId).status()).isEqualTo("EXECUTED");
        assertThat(getExecution.execute(userId, applied.executionId()).items()).usingRecursiveComparison().isEqualTo(applied.items());
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::quantity).containsExactly(5);
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 제안_기준이_달라지면_무효화하고_같은_키는_같은_거절을_재생한다() {
        add.execute(new AddCartItemCommand(userId, firstProductId, 3));
        final Long proposalId =
            prepare.createProposal(command("invalidated", List.of(line(CartChangeOperation.UPDATE, firstProductId, 3, 2))));
        modify.execute(new ModifyCartItemCommand(userId, cartItemId(firstProductId), 5));

        final CartChangeResult rejected = execute.confirm(userId, proposalId, "confirm-conflict");
        modify.execute(new ModifyCartItemCommand(userId, cartItemId(firstProductId), 3));
        final CartChangeResult replay = execute.confirm(userId, proposalId, "confirm-conflict");
        final CartChangeResult otherKey = execute.confirm(userId, proposalId, "confirm-invalidated");

        assertThat(rejected.rejection()).isEqualTo(ErrorCode.CART_STATE_CHANGED);
        assertThat(replay.rejection()).isEqualTo(ErrorCode.CART_STATE_CHANGED);
        assertThat(replay.replayed()).isTrue();
        assertThat(otherKey.rejection()).isEqualTo(ErrorCode.PROPOSAL_NOT_EXECUTABLE);
        assertThat(getProposal.execute(userId, proposalId).status()).isEqualTo("INVALIDATED");
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::quantity).containsExactly(3);
        assertThat(executionCount()).isZero();
    }

    @Test
    void 다른_사용자는_요청_제안_실행을_조회하거나_실행할_수_없다() {
        final long otherUserId = userId + 9;
        jdbc.update("INSERT INTO carts (user_id) VALUES (?)", otherUserId);
        final Long requestId =
            prepare.prepareDirect(command("owned-request", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        final Long proposalId =
            prepare.createProposal(command("owned-proposal", List.of(line(CartChangeOperation.ADD, secondProductId, 0, 1))));

        assertNotFound(() -> execute.execute(otherUserId, requestId));
        assertNotFound(() -> execute.confirm(otherUserId, proposalId, "foreign-confirm"));
        assertNotFound(() -> getProposal.execute(otherUserId, proposalId));
        assertThat(getCart.execute(userId).items()).isEmpty();
        final CartChangeResult applied = execute.execute(userId, requestId);
        assertNotFound(() -> getExecution.execute(otherUserId, applied.executionId()));
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void ISBN이_없고_상품과_도서가_삭제되어도_가격과_재고_없이_제거한다() {
        add.execute(new AddCartItemCommand(userId, firstProductId, 3));
        jdbc.update("UPDATE products SET deleted_at = CURRENT_TIMESTAMP(6), stock_quantity = 0 WHERE id = ?", firstProductId);
        jdbc.update("UPDATE books SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", firstProductId + 1000000);
        final Long requestId = prepare.prepareDirect(
            command("remove-deleted", List.of(new CartChangeLine(CartChangeOperation.REMOVE, firstProductId, 3, null, null))));

        final CartChangeResult result = execute.execute(userId, requestId);

        assertThat(result.rejection()).isNull();
        assertThat(result.executionId()).isNotNull();
        assertThat(getCart.execute(userId).items()).isEmpty();
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 실행기록_저장이_실패하면_항목변경과_제안소비를_롤백하고_재시도할_수_있다() {
        final Long proposalId = prepare.createProposal(command("rollback",
            List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2), line(CartChangeOperation.ADD, secondProductId, 0, 1))));
        jdbc.execute("ALTER TABLE cart_executions ADD CONSTRAINT fail_test_execution CHECK (user_id <> " + userId + ")");
        try {
            assertThatThrownBy(() -> execute.confirm(userId, proposalId, "confirm-rollback")).isInstanceOf(DataAccessException.class);
        } finally {
            jdbc.execute("ALTER TABLE cart_executions DROP CHECK fail_test_execution");
        }

        assertThat(getCart.execute(userId).items()).isEmpty();
        assertThat(getProposal.execute(userId, proposalId).status()).isEqualTo("PENDING");
        assertThat(executionCount()).isZero();
        final CartChangeResult retried = execute.confirm(userId, proposalId, "confirm-rollback");
        assertThat(retried.rejection()).isNull();
        assertThat(getCart.execute(userId).items()).extracting(GetCartItemResult::productId, GetCartItemResult::quantity)
            .containsExactlyInAnyOrder(tuple(firstProductId, 2), tuple(secondProductId, 1));
        assertThat(getProposal.execute(userId, proposalId).status()).isEqualTo("EXECUTED");
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 같은_원본을_직접_지시와_확인_제안으로_바꿔_실행하지_못한다() {
        final PrepareCartChangeCommand proposalCommand =
            command("frozen-proposal", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2)));
        prepare.createProposal(proposalCommand);
        assertThatThrownBy(() -> prepare.prepareDirect(proposalCommand)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
        final PrepareCartChangeCommand directCommand =
            command("frozen-direct", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2)));
        prepare.prepareDirect(directCommand);
        assertThatThrownBy(() -> prepare.createProposal(directCommand)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Test
    void 잠금_후_최종_검증_시각에_만료했다면_변경하지_않고_거절을_확정한다() {
        final Long proposalId =
            prepare.createProposal(command("expires-during-lock", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        when(clock.instant()).thenReturn(NOW.plusSeconds(599), NOW.plusSeconds(600));
        final CartChangeResult result = execute.confirm(userId, proposalId, "expired-confirmation");
        assertThat(result.rejection()).isEqualTo(ErrorCode.PROPOSAL_NOT_EXECUTABLE);
        assertThat(getCart.execute(userId).items()).isEmpty();
        assertThat(execute.confirm(userId, proposalId, "expired-confirmation").replayed()).isTrue();
    }

    @Test
    void 항목_DB제약은_ADD_UPDATE의_null_수량을_거절한다() {
        final Long requestId = prepare.prepareDirect(command("db-quantity", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        assertThatThrownBy(() -> jdbc.update("UPDATE cart_change_request_items SET quantity = NULL WHERE request_id = ?", requestId))
            .isInstanceOf(DataAccessException.class);
        final Long proposalId =
            prepare.createProposal(command("db-proposal", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        assertThatThrownBy(() -> jdbc.update("UPDATE cart_proposal_items SET quantity = NULL WHERE proposal_id = ?", proposalId))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    void 오래된_스냅샷에서_새_READY와_제안은_충돌하고_새_트랜잭션에서_복구한다() {
        final TransactionTemplate outer = new TransactionTemplate(transactionManager);
        final TransactionTemplate independent = new TransactionTemplate(transactionManager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        final AtomicLong requestId = new AtomicLong();
        assertThatThrownBy(() -> outer.executeWithoutResult((final TransactionStatus status) -> {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_change_requests WHERE user_id = ?", Integer.class, userId)).isZero();
            requestId.set(independent.execute((final TransactionStatus other) -> prepare
                .prepareDirect(command("new-ready", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))))));
            execute.execute(userId, requestId.get());
        })).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(execute.execute(userId, requestId.get()).rejection()).isNull();
        final AtomicLong proposalId = new AtomicLong();
        assertThatThrownBy(() -> outer.executeWithoutResult((final TransactionStatus status) -> {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_proposals WHERE user_id = ?", Integer.class, userId)).isZero();
            proposalId.set(independent.execute((final TransactionStatus other) -> prepare
                .createProposal(command("new-proposal", List.of(line(CartChangeOperation.ADD, secondProductId, 0, 1))))));
            execute.confirm(userId, proposalId.get(), "new-confirm");
        })).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(execute.confirm(userId, proposalId.get(), "new-confirm").rejection()).isNull();
        assertThat(getCart.execute(userId).items()).hasSize(2);
    }

    @Test
    void 캐시된_READY는_경합하면_롤백하고_새_트랜잭션은_성공을_재생한다() {
        final Long requestId =
            prepare.prepareDirect(command("cached-request", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        final TransactionTemplate outer = new TransactionTemplate(transactionManager);
        final TransactionTemplate independent = new TransactionTemplate(transactionManager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        final AtomicLong executionId = new AtomicLong();
        assertThatThrownBy(() -> outer.executeWithoutResult((final TransactionStatus status) -> {
            assertThat(changes.findRequest(userId, requestId).orElseThrow().executionId()).isNull();
            executionId.set(independent.execute((final TransactionStatus other) -> execute.execute(userId, requestId)).executionId());
            execute.execute(userId, requestId);
        })).isInstanceOf(OptimisticLockingFailureException.class);
        final CartChangeResult replayed = execute.execute(userId, requestId);
        assertThat(replayed.executionId()).isEqualTo(executionId.get());
        assertThat(replayed.replayed()).isTrue();
        assertThat(executionCount()).isEqualTo(1);
    }

    @Test
    void 종류_한도는_여러_상품의_최종_상태로_검증한다() {
        for (int index = 0; index < 30; index++) {
            final long productId = userId + 100 + index;
            createProduct(productId, 100);
            add.execute(new AddCartItemCommand(userId, productId, 1));
        }
        final Long overLimit = prepare.prepareDirect(command("over-limit", List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        assertThat(execute.execute(userId, overLimit).rejection()).isEqualTo(ErrorCode.CART_ITEM_LIMIT_EXCEEDED);
        assertThat(getCart.execute(userId).items()).hasSize(30);

        final Long replace =
            prepare.prepareDirect(command("free-slot", List.of(new CartChangeLine(CartChangeOperation.REMOVE, userId + 100, 1, null, null),
                line(CartChangeOperation.ADD, firstProductId, 0, 2))));
        assertThat(execute.execute(userId, replace).rejection()).isNull();
        assertThat(getCart.execute(userId).items()).hasSize(30);
    }

    @Test
    void 상품_커밋_충돌은_장바구니_제안소비_성공기록을_함께_롤백한다() {
        final Long proposalId = prepare.createProposal(command("commit-conflict",
            List.of(line(CartChangeOperation.ADD, firstProductId, 0, 2), line(CartChangeOperation.ADD, secondProductId, 0, 1))));
        final TransactionTemplate independent = new TransactionTemplate(transactionManager);
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult((final TransactionStatus status) -> {
            assertThat(execute.confirm(userId, proposalId, "commit-key").rejection()).isNull();
            independent.executeWithoutResult((final TransactionStatus other) -> jdbc
                .update("UPDATE products SET discounted_price = 19000, version = version + 1 WHERE id = ?", secondProductId));
        })).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(getCart.execute(userId).items()).isEmpty();
        assertThat(executionCount()).isZero();
        assertThat(getProposal.execute(userId, proposalId).status()).isEqualTo("PENDING");
        assertThat(execute.confirm(userId, proposalId, "commit-key").rejection()).isEqualTo(ErrorCode.CART_PRICE_CHANGED);
        assertThat(getCart.execute(userId).items()).isEmpty();
    }

    private PrepareCartChangeCommand command(final String key, final List<CartChangeLine> items) {
        return new PrepareCartChangeCommand(userId, key, items);
    }

    private static CartChangeLine line(final CartChangeOperation operation, final long productId, final int expectedQuantity,
        final int quantity) {
        return new CartChangeLine(operation, productId, expectedQuantity, quantity, UNIT_PRICE);
    }

    private static List<CartChangeResult> concurrently(final Supplier<CartChangeResult> first, final Supplier<CartChangeResult> second)
        throws Exception {
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        try (final ExecutorService workers = Executors.newFixedThreadPool(2)) {
            final Future<CartChangeResult> firstResult = workers.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return concurrentAttempt(first);
            });
            final Future<CartChangeResult> secondResult = workers.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return concurrentAttempt(second);
            });
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            return Stream.of(firstResult.get(20, TimeUnit.SECONDS), secondResult.get(20, TimeUnit.SECONDS)).filter(Objects::nonNull)
                .toList();
        }
    }

    private static CartChangeResult concurrentAttempt(final Supplier<CartChangeResult> action) {
        try {
            return action.get();
        } catch (final OptimisticLockingFailureException conflict) {
            return null; // 상대 요청만 성공하며 패자는 자동 재실행하지 않습니다.
        }
    }

    private static void assertNotFound(final Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.CART_CHANGE_NOT_FOUND));
    }

    private void createProduct(final long productId, final int stockQuantity) {
        final long bookId = productId + 1000000;
        jdbc.update("""
                INSERT INTO books (id, title, author, publisher, category, published_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, bookId, "도서" + bookId, "작가", "출판사", "소설", "2026-01-01");
        jdbc.update("""
                INSERT INTO products (
                    id, book_id, name, sale_price, discounted_price, cost_price, stock_quantity, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, productId, bookId, "상품" + productId, new BigDecimal("20000.00"), UNIT_PRICE, new BigDecimal("12000.00"),
            stockQuantity);
    }

    private Long cartItemId(final long productId) {
        return getCart.execute(userId).items().stream().filter((final GetCartItemResult item) -> item.productId().equals(productId))
            .findFirst().orElseThrow().cartItemId();
    }

    private int executionCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM cart_executions WHERE user_id = ?", Integer.class, userId);
    }
}
