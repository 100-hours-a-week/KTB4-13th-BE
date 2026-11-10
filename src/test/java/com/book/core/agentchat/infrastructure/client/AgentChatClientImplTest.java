package com.book.core.agentchat.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.config.ai.AiChatHttpClient;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentCheckout;
import com.book.core.agentchat.application.result.AgentContextCard;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.onboarding.application.result.RecommendationHistory;
import com.book.core.onboarding.application.usecase.GetRecommendationHistoryUseCase;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.RecommendationSpec;
import com.book.core.recommendation.application.command.RecommendationTurn;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.domain.enums.CouponType;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class AgentChatClientImplTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.invalid");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GetAiBookIdentifiersUseCase identifiers = mock(GetAiBookIdentifiersUseCase.class);
    private final GetRecommendationHistoryUseCase history = mock(GetRecommendationHistoryUseCase.class);
    private final AiChatHttpClient http = mock(AiChatHttpClient.class);
    private final AgentChatClientImpl client = new AgentChatClientImpl(http, new AgentChatProperties(true),
        new AgentChatExternalMapper(identifiers), identifiers, history, new ObjectMapper());
    private final ChatRecommendationCommand command = new ChatRecommendationCommand(7L, true, Map.of(), "1번 5권 담아줘",
        List.of(new RecommendationTurn("user", "추천"), new RecommendationTurn("assistant", "추천합니다")), List.of());
    private final AgentChatContext context =
        new AgentChatContext(List.of(new AgentObservedProduct(10L, "9788900000011", "책", new BigDecimal("12000.00"), 2, 3, true)),
            List.of(new AgentContextCard("9788900000011", 1, 90)), List.of(), BigDecimal.ZERO);

    @BeforeEach
    void prepareCatalogAndHistory() {
        when(http.before(anyLong())).thenReturn(builder.build());
        when(identifiers.findUnambiguousIsbns(List.of("9788900000011"))).thenReturn(Set.of("9788900000011"));
        when(history.execute(7L)).thenReturn(RecommendationHistory.NOT_CONSENTED);
    }

    @Test
    void 식별할_수_없는_장바구니_책은_AI에_보내지_않고_일반_추천으로_진행한다() {
        when(identifiers.findUnambiguousIsbns(List.of("9788900000011"))).thenReturn(Set.of());
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(jsonPath("$.idempotency_key").doesNotExist())
            .andExpect(jsonPath("$.cart").isEmpty()).andExpect(jsonPath("$.offers").isEmpty())
            .andExpect(jsonPath("$.owned_coupons").isEmpty()).andExpect(jsonPath("$.point_balance").value(0))
            .andExpect(jsonPath("$.context_cards").isEmpty()).andRespond(withSuccess("""
                {"data":{"reply":"추천합니다","spec":{},"cards":[],"buttons":[],"degraded":false}}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.chat(command, 8L, 19L, context).route()).isEqualTo("recommend");
        server.verify();
    }

    @Test
    void 기존_입구에_최초_장바구니_가격_재고숫자와_서버_식별자를_보낸다() {
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(jsonPath("$.user_id").value(7))
            .andExpect(jsonPath("$.idempotency_key").value("chat_19")).andExpect(jsonPath("$.conversation_id").value("cv_8"))
            .andExpect(jsonPath("$.user_context.logged_in").value(true)).andExpect(jsonPath("$.offers[0].stock_quantity").value(3))
            .andExpect(jsonPath("$.cart[0].quantity").value(2)).andExpect(jsonPath("$.context_cards[0].match_score").value(90))
            .andExpect(jsonPath("$.recent_turns[0].text").value("추천")).andRespond(withSuccess("""
                {"message":"agent_success","data":{"route":"shop","reply":"담았습니다",
                "actions":[{"op":"set","isbn":"9788900000011","quantity":5}],
                "buttons":[{"label":"장바구니 보기","action":"navigate","target":"CART-001"}]}}
                """, MediaType.APPLICATION_JSON));
        assertThat(client.chat(command, 8L, 19L, context).actions().getFirst().quantity()).isEqualTo(5);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void 상품_ID와_쿠폰과_포인트를_보내고_목록_밖_ISBN_담기와_주문_이동을_받는다(final boolean stream) {
        final CouponResult coupon = new CouponResult(3L, "10% 할인", CouponType.PERCENT_RATE, new BigDecimal("0.10"), new BigDecimal("30000"),
            LocalDateTime.parse("2026-11-30T23:59:59"));
        final AgentChatContext withBenefits = new AgentChatContext(context.products(), context.cards(),
            List.of(new OwnedCouponResult(7L, 7L, OwnedCouponStatus.ISSUED, 1, 0, coupon)), new BigDecimal("5000.00"));
        final String reply = """
            {"data":{"route":"shop","reply":"담았습니다","actions":[{"op":"set","isbn":"9788900000028","quantity":1}],
            "buttons":[{"label":"주문 화면","action":"navigate","target":"ORDER-001"}],"checkout":{"owned_coupon_id":7,"use_point":5000}}}
            """.strip().replace("\n", "");
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(jsonPath("$.cart[0].product_id").value(10))
            .andExpect(jsonPath("$.offers[0].product_id").value(10)).andExpect(jsonPath("$.owned_coupons[0].owned_coupon_id").value(7))
            .andExpect(jsonPath("$.owned_coupons[0].coupon_id").value(3))
            .andExpect(jsonPath("$.owned_coupons[0].type").value("PERCENT_RATE"))
            .andExpect(jsonPath("$.owned_coupons[0].discount").value(0.1))
            .andExpect(jsonPath("$.owned_coupons[0].min_order_amount").value(30000))
            .andExpect(jsonPath("$.owned_coupons[0].expired_at").value("2026-11-30T23:59:59"))
            .andExpect(jsonPath("$.point_balance").value(5000))
            .andRespond(stream ? withSuccess("event: done\ndata: " + reply + "\n\n", MediaType.TEXT_EVENT_STREAM)
                : withSuccess(reply, MediaType.APPLICATION_JSON));
        final var decision = stream ? client.chat(command, 8L, 19L, withBenefits, (final String text) -> {
            throw new AssertionError("쇼핑 완료 전에 delta가 전송되었습니다");
        }) : client.chat(command, 8L, 19L, withBenefits);
        assertThat(decision.actions().getFirst().isbn()).isEqualTo("9788900000028");
        assertThat(decision.checkout()).isEqualTo(new AgentCheckout(7L, new BigDecimal("5000")));
        assertThat(decision.buttons().getFirst().opensCheckout()).isTrue();
        server.verify();
    }

    @Test
    void 쇼핑_SSE는_delta_없이_done으로_끝나도_기존_쇼핑_응답을_만든다() {
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(header(HttpHeaders.ACCEPT, "text/event-stream"))
            .andRespond(withSuccess("""
                event: done
                data: {"message":"agent_success","data":{"route":"shop","reply":"담았습니다","actions":[],"buttons":[]}}

                """, MediaType.TEXT_EVENT_STREAM));

        assertThat(client.chat(command, 8L, 19L, context, (final String text) -> {
            throw new AssertionError("쇼핑 완료 전에 delta가 전송되었습니다");
        }).route()).isEqualTo("shop");
        server.verify();
    }

    @Test
    void 스트림_시작_전_HTTP_422는_초기_spec으로_재시도하고_재시도_조각만_전달한다() {
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(header(HttpHeaders.ACCEPT, "text/event-stream"))
            .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"spec_schema_violation\"}"));
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(header(HttpHeaders.ACCEPT, "text/event-stream"))
            .andExpect(jsonPath("$.spec").value(org.hamcrest.Matchers.equalTo(RecommendationSpec.initial())))
            .andExpect(jsonPath("$.cart[0].product_id").value(10)).andExpect(jsonPath("$.offers[0].product_id").value(10))
            .andRespond(withSuccess("""
                event: delta
                data: {"text":"새 답변"}

                event: done
                data: {"message":"agent_success","data":{"route":"shop","reply":"새 답변","actions":[],"buttons":[]}}

                """, MediaType.TEXT_EVENT_STREAM));

        final List<String> deltas = new ArrayList<>();
        assertThat(client.chat(command, 8L, 19L, context, deltas::add).reply()).isEqualTo("새 답변");
        assertThat(deltas).containsExactly("새 답변");
        final ArgumentCaptor<Long> deadline = ArgumentCaptor.forClass(Long.class);
        verify(http, times(2)).before(deadline.capture());
        assertThat(deadline.getAllValues()).containsOnly(deadline.getValue());
        server.verify();
    }

    @Test
    void delta_이후_스키마_오류_이벤트는_같은_스트림에서_재시도하지_않는다() {
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(header(HttpHeaders.ACCEPT, "text/event-stream"))
            .andRespond(withSuccess("""
                event: delta
                data: {"text":"임시 답변"}

                event: error
                data: {"message":"spec_schema_violation"}

                """, MediaType.TEXT_EVENT_STREAM));

        final List<String> deltas = new ArrayList<>();
        assertThatThrownBy(() -> client.chat(command, 8L, 19L, context, deltas::add)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION));
        assertThat(deltas).containsExactly("임시 답변");
        server.verify();
    }

    @Test
    void 배포_확인_전_기본값은_비활성이고_호출을_허용하지_않는다() {
        final AgentChatClientImpl disabled = new AgentChatClientImpl(http, new AgentChatProperties(false),
            new AgentChatExternalMapper(identifiers), identifiers, history, new ObjectMapper());
        assertThat(disabled.enabled()).isFalse();
        assertThatThrownBy(() -> disabled.chat(command, 8L, 19L, context)).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"route\":\"unknown\",\"reply\":\"x\",\"buttons\":[]}",
        "{\"route\":\"shop\",\"reply\":\"x\",\"actions\":[],"
            + "\"buttons\":[{\"label\":\"x\",\"action\":\"navigate\",\"target\":\"https://bad.invalid\"}]}",
        "{\"route\":\"shop\",\"reply\":\"x\",\"actions\":[{\"op\":\"set\",\"isbn\":\"9788900000011\",\"quantity\":501}],\"buttons\":[]}",
        "{\"route\":\"shop\",\"reply\":\"x\",\"actions\":[{\"op\":\"set\",\"isbn\":\"9788900000011\",\"quantity\":1.5}],\"buttons\":[]}",
        "{\"route\":\"shop\",\"reply\":\"x\",\"actions\":[],\"selection\":{\"chosen\":[{\"isbn\":\"9788900000011\",\"price\":12000}]},"
            + "\"buttons\":[]}",
        "{\"route\":\"shop\",\"reply\":\"x\",\"actions\":[],\"spec\":{},\"buttons\":[]}"})
    void HTTP200이어도_계약에_어긋난_응답은_실행_후보로_사용하지_않는다(final String data) {
        server.expect(requestTo("https://ai.invalid/recommendations/chat"))
            .andRespond(withSuccess("{\"data\":" + data + "}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.chat(command, 8L, 19L, context)).isInstanceOf(CoreException.class);
        server.verify();
    }

    @Test
    void 호출_제한의_재시도_간격을_유지하고_자동_재호출하지_않는다() {
        server.expect(requestTo("https://ai.invalid/recommendations/chat"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "17"));
        assertThatThrownBy(() -> client.chat(command, 8L, 19L, context)).isInstanceOfSatisfying(RetryAfterException.class,
            (final RetryAfterException exception) -> assertThat(exception.retryAfterSeconds()).isEqualTo(17));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"route\":\"shop\",\"reply\":\"어떤 책인가요?\",\"actions\":[],\"buttons\":[]}",
        "{\"route\":\"recommend\",\"reply\":\"추천합니다\",\"spec\":{},\"cards\":[],\"buttons\":[],\"degraded\":false}"})
    void 조건_위반만_초기_spec으로_한번_재시도하고_키와_최초_관측은_유지한다(final String data) {
        server.expect(requestTo("https://ai.invalid/recommendations/chat")).andExpect(jsonPath("$.spec").isMap())
            .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).body("{\"success\":false,\"message\":\"spec_schema_violation\"}")
                .contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://ai.invalid/recommendations/chat"))
            .andExpect(jsonPath("$.spec").value(org.hamcrest.Matchers.equalTo(RecommendationSpec.initial())))
            .andExpect(jsonPath("$.idempotency_key").value("chat_19")).andExpect(jsonPath("$.conversation_id").value("cv_8"))
            .andExpect(jsonPath("$.cart[0].quantity").value(2)).andExpect(jsonPath("$.cart[0].price").value(12000))
            .andExpect(jsonPath("$.offers[0].stock_quantity").value(3)).andExpect(jsonPath("$.message").value(command.message()))
            .andRespond(withSuccess("{\"data\":" + data + "}", MediaType.APPLICATION_JSON));
        assertThat(client.chat(command, 8L, 19L, context).reply()).isNotBlank();
        server.verify();
    }

    @Test
    void 조건_위반이_두번이면_명세_위반으로_끝내고_세번째_호출은_하지_않는다() {
        server.expect(twice(), requestTo("https://ai.invalid/recommendations/chat")).andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
            .body("{\"message\":\"spec_schema_violation\"}").contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.chat(command, 8L, 19L, context)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_RECOMMENDATION_SPEC_VIOLATION));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"message\":\"invalid_request\"}", "{", "{}"})
    void 다른_422나_해석불가_본문에는_재시도하지_않는다(final String body) {
        server.expect(requestTo("https://ai.invalid/recommendations/chat"))
            .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).body(body).contentType(MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.chat(command, 8L, 19L, context)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
        server.verify();
    }

}
