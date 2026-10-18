package com.book.core.agentchat.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.ConfirmCartProposalCommand;
import com.book.core.agentchat.application.command.GetChatHistoryCommand;
import com.book.core.agentchat.application.command.SendChatMessageCommand;
import com.book.core.agentchat.application.port.AgentChatClient;
import com.book.core.agentchat.application.port.ChatResponseCodec;
import com.book.core.agentchat.application.result.AgentAction;
import com.book.core.agentchat.application.result.AgentButton;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentChosenBook;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentSelection;
import com.book.core.agentchat.application.result.CartChangeResult;
import com.book.core.agentchat.application.result.ChatResponseSnapshot;
import com.book.core.agentchat.application.service.AgentCartService;
import com.book.core.agentchat.application.service.ChatSessionService;
import com.book.core.agentchat.infrastructure.persistence.mapper.JacksonChatResponseCodec;
import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import com.book.core.product.application.result.ProductOfferResult;
import com.book.core.product.application.usecase.GetProductOffersUseCase;
import com.book.core.recommendation.application.command.RecommendationCardCandidate;
import com.book.core.recommendation.application.command.ResolveRecommendationCommand;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class ShoppingAgentChatIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(2800000);
    private static final Instant NOW = Instant.parse("2026-10-19T00:00:00Z");
    private static final String ISBN = "9788900000011";
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @Autowired
    ChatSessionService chats;
    @Autowired
    AgentCartService agentCart;
    @Autowired
    GetCartUseCase carts;
    @Autowired
    AddCartItemUseCase add;
    @Autowired
    ModifyCartItemUseCase modify;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    GetProductOffersUseCase products;
    @Autowired
    EntityManagerFactory entityManagers;
    @MockitoBean
    AgentChatClient ai;
    @MockitoBean
    Clock clock;
    @MockitoBean
    ChatResponseCodec codec;
    private final ChatResponseCodec realCodec = new JacksonChatResponseCodec(JsonMapper.builder().build());
    private long userId;
    private Long sessionId;

    @BeforeEach
    void setUp() {
        userId = IDS.incrementAndGet();
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(codec.encode(any())).thenAnswer((final InvocationOnMock call) -> realCodec.encode(call.getArgument(0)));
        when(codec.decode(any())).thenAnswer((final InvocationOnMock call) -> realCodec.decode(call.getArgument(0)));
        when(ai.enabled()).thenReturn(true);
        jdbc.update("INSERT INTO users (id,nickname) VALUES (?,?)", userId, "shop" + userId);
        jdbc.update("INSERT INTO carts (id,user_id) VALUES (?,?)", userId, userId);
        jdbc.update(
            "INSERT INTO books (id,isbn13,title,author,publisher,category,published_at) VALUES (?,?,'책','작가','출판사','소설','2026-01-01')",
            userId, ISBN);
        jdbc.update("INSERT INTO products (id,book_id,name,sale_price,discounted_price,cost_price,stock_quantity) "
            + "VALUES (?,?,'상품',15000,12000,9000,3)", userId, userId);
        sessionId = chats.create(userId);
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenAnswer((final InvocationOnMock call) -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new AgentDecision("recommend", "추천합니다",
                new ResolveRecommendationCommand(Map.of("intent", "semantic"), "추천합니다",
                    List.of(new RecommendationCardCandidate(userId, 90, "추천", "추천 이유")), null, List.of(), true),
                List.of(), null, List.of());
        });
        chats.send(command("seed", "추천해 줘"));
    }

    @Test
    void 직접_변경과_실제_응답을_함께_저장하고_응답_유실은_재실행하지_않는다() {
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenReturn(shop(2));
        final ChatResponseSnapshot sent = chats.send(command("set", "1번 2권 담아줘"));
        assertThat(sent.route()).isEqualTo("shop");
        assertThat(sent.execution().status()).isEqualTo("applied");
        assertThat(sent.spec()).containsEntry("intent", "semantic");
        assertThat(sent.cards()).isEmpty();
        final Long itemId = carts.execute(userId).items().getFirst().cartItemId();
        modify.execute(new ModifyCartItemCommand(userId, itemId, 3));
        assertThat(chats.send(command("set", "1번 2권 담아줘"))).isEqualTo(sent);
        assertThat(carts.execute(userId).items().getFirst().quantity()).isEqualTo(3);
        verify(ai, times(2)).chat(any(), anyLong(), anyLong(), any());
        assertThat(chats.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(4);
    }

    @Test
    void AI200_후_재고_거절은_사용자와_BE_안내를_저장한다() {
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenReturn(shop(5));
        final ChatResponseSnapshot response = chats.send(command("stock", "1번 5권 담아줘"));
        assertThat(response.execution().status()).isEqualTo("rejected");
        assertThat(response.reply()).isEqualTo(ErrorCode.INSUFFICIENT_PRODUCT_STOCK.message());
        assertThat(carts.execute(userId).items()).isEmpty();
        assertThat(chats.history(new GetChatHistoryCommand(userId, sessionId, 0, 100)).getLast().text()).isEqualTo(response.reply());
    }

    @Test
    void AI_대기_중_일반_변경이_있으면_최초_기준을_덮어쓰지_않는다() {
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenAnswer((final InvocationOnMock call) -> {
            final AgentChatContext context = call.getArgument(3);
            assertThat(context.requireProduct(ISBN).quantity()).isZero();
            add.execute(new AddCartItemCommand(userId, userId, 3));
            return shop(2);
        });
        final ChatResponseSnapshot response = chats.send(command("race", "1번 2권 담아줘"));
        assertThat(response.execution().rejectionCode()).isEqualTo("CART_STATE_CHANGED");
        assertThat(carts.execute(userId).items().getFirst().quantity()).isEqualTo(3);
    }

    @Test
    void 제안_버튼은_BE_식별자로_연결하고_FE_확인은_같은_성공을_재생한다() {
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenReturn(new AgentDecision("shop", "이 책을 담을까요?", null, List.of(),
            new AgentSelection(List.of(new AgentChosenBook(ISBN, new BigDecimal("12000"))), new BigDecimal("20000"), 1),
            List.of(new AgentButton("담기", "confirm_selection", null, null))));
        final ChatResponseSnapshot response = chats.send(command("proposal", "예산 안에서 골라줘"));
        final Long proposalId = response.agentButtons().getFirst().proposalId();
        assertThat(proposalId).isNotNull();
        assertThat(carts.execute(userId).items()).isEmpty();
        final ConfirmCartProposalCommand confirmation = new ConfirmCartProposalCommand(userId, proposalId, "confirm");
        final CartChangeResult first = agentCart.confirm(confirmation);
        assertThat(agentCart.confirm(confirmation).executionId()).isEqualTo(first.executionId());
        assertThat(carts.execute(userId).items().getFirst().quantity()).isEqualTo(1);
    }

    @Test
    void 저장_실패는_변경도_롤백하고_저장한_AI후보로_재호출_없이_복구한다() {
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenReturn(shop(2));
        when(codec.encode(any())).thenThrow(new IllegalStateException("snapshot failure"));
        assertThatThrownBy(() -> chats.send(command("recover", "1번 2권 담아줘"))).isInstanceOf(IllegalStateException.class);
        assertThat(carts.execute(userId).items()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cart_executions WHERE user_id = ?", Integer.class, userId)).isZero();
        Mockito.doAnswer((final InvocationOnMock call) -> realCodec.encode(call.getArgument(0))).when(codec).encode(any());
        when(ai.enabled()).thenReturn(false);
        when(clock.instant()).thenReturn(NOW.plusSeconds(121));
        assertThat(chats.send(command("recover", "1번 2권 담아줘")).execution().status()).isEqualTo("applied");
        verify(ai, times(2)).chat(any(), anyLong(), anyLong(), any());
    }

    @Test
    void AI_실패는_발화쌍을_제외하지만_정상_업무_안내는_남긴다() {
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenThrow(new CoreException(ErrorCode.AI_RECOMMENDATION_TIMEOUT));
        assertThatThrownBy(() -> chats.send(command("timeout", "담아줘"))).isInstanceOf(CoreException.class);
        assertThat(chats.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(2);
        Mockito.doReturn(new AgentDecision("shop", "이미 3권 담겨 있어요", null, List.of(), null, List.of())).when(ai).chat(any(), anyLong(),
            anyLong(), any());
        assertThat(chats.send(command("notice", "담아줘")).reply()).isEqualTo("이미 3권 담겨 있어요");
        assertThat(chats.history(new GetChatHistoryCommand(userId, sessionId, 0, 100))).hasSize(4);
    }

    @Test
    void AI_대기_중_가격이_바뀌면_거절하고_새_가격으로_기준을_바꾸지_않는다() {
        when(ai.chat(any(), anyLong(), anyLong(), any())).thenAnswer((final InvocationOnMock call) -> {
            jdbc.update("UPDATE products SET discounted_price=13000, version=version+1 WHERE id=?", userId);
            return shop(2);
        });
        final ChatResponseSnapshot response = chats.send(command("price", "1번 2권 담아줘"));
        assertThat(response.execution().status()).isEqualTo("rejected");
        assertThat(carts.execute(userId).items()).isEmpty();
        assertThat(response.reply()).isNotEqualTo("2권 담았어요");
    }

    @Test
    void 삭제_후_과거_응답을_열어도_당시_카드와_문장을_복원한다() {
        add.execute(new AddCartItemCommand(userId, userId, 2));
        when(ai.chat(any(), anyLong(), anyLong(), any()))
            .thenReturn(new AgentDecision("shop", "뺐습니다", null, List.of(new AgentAction("remove", ISBN, null)), null, List.of()));
        assertThat(chats.send(command("remove", "1번 빼줘")).execution().status()).isEqualTo("applied");
        assertThat(carts.execute(userId).items()).isEmpty();
        jdbc.update("UPDATE products SET discounted_price=13000 WHERE id=?", userId);
        assertThat(chats.history(new GetChatHistoryCommand(userId, sessionId, 0, 100)).get(1).response().cards().getFirst().price())
            .isEqualByComparingTo("12000");
    }

    @Test
    void 여러_상품_정보는_책까지_한_쿼리로_조회하고_중복과_누락을_제외한다() {
        final Statistics statistics = entityManagers.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        try {
            final long before = statistics.getPrepareStatementCount();
            final List<ProductOfferResult> offers = products.execute(List.of(userId, -1L, userId));
            assertThat(statistics.getPrepareStatementCount() - before).isEqualTo(1);
            assertThat(offers).hasSize(1);
            assertThat(offers.getFirst().isbn()).isEqualTo(ISBN);
            assertThat(offers.getFirst().stockQuantity()).isEqualTo(3);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    private AgentDecision shop(final int quantity) {
        return new AgentDecision("shop", quantity + "권 담았어요", null, List.of(new AgentAction("set", ISBN, quantity)), null,
            List.of(new AgentButton("장바구니 보기", "navigate", "CART-001", null)));
    }

    private SendChatMessageCommand command(final String key, final String text) {
        return new SendChatMessageCommand(userId, sessionId, key, text, true, List.of());
    }
}
