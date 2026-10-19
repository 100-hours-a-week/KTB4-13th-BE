package com.book.core.agentchat.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.exception.RetryAfterException;
import com.book.core.agentchat.application.result.AgentChatContext;
import com.book.core.agentchat.application.result.AgentContextCard;
import com.book.core.agentchat.application.result.AgentObservedProduct;
import com.book.core.recommendation.application.command.ChatRecommendationCommand;
import com.book.core.recommendation.application.command.RecommendationTurn;
import com.book.core.recommendation.application.command.RecommendationSpec;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AgentChatClientImplTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://ai.invalid");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AgentChatClientImpl client =
        new AgentChatClientImpl(builder.build(), new AgentChatProperties(true), new AgentChatExternalMapper());
    private final ChatRecommendationCommand command = new ChatRecommendationCommand(7L, true, Map.of(), "1번 5권 담아줘",
        List.of(new RecommendationTurn("user", "추천"), new RecommendationTurn("assistant", "추천합니다")), List.of());
    private final AgentChatContext context =
        new AgentChatContext(List.of(new AgentObservedProduct(10L, "9788900000011", "책", new BigDecimal("12000.00"), 2, 3, true)),
            List.of(new AgentContextCard("9788900000011", 1, 90)));

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

    @Test
    void 배포_확인_전_기본값은_비활성이고_호출을_허용하지_않는다() {
        final AgentChatClientImpl disabled =
            new AgentChatClientImpl(builder.build(), new AgentChatProperties(false), new AgentChatExternalMapper());
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
