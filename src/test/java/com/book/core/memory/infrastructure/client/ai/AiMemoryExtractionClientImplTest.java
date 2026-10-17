package com.book.core.memory.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.command.ConversationMessage;
import com.book.core.memory.application.command.UserMemoryExtraction;
import com.book.core.memory.application.port.AiMemoryExtractionRequest;
import com.book.core.memory.domain.UserMemoryType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiMemoryExtractionClientImplTest {
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String EXTRACTION_URL = BASE_URL + "/preferences/extractions";
    private static final String CONVERSATION_ID = "cv_20261013_a1";
    private static final String EXTRACTION = """
        {"type":"topic","value":"추리 소설을 좋아함","strength":2,"applies_to":["추리/스릴러"],"keyword":"추리 소설",
         "vector":[0.054,-0.25],"dim":2,"source_conversation_id":"cv_20261013_a1"}""";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AiMemoryExtractionClientImpl client = new AiMemoryExtractionClientImpl(builder.build());

    @Test
    void 대화를_AI_계약의_snake_case로_보내고_추출_결과를_도메인_값으로_변환한다() {
        server.expect(requestTo(EXTRACTION_URL)).andExpect(method(HttpMethod.POST)).andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.consented").value(true)).andExpect(jsonPath("$.conversation_id").value(CONVERSATION_ID))
            .andExpect(jsonPath("$.conversation[0].role").value("user")).andExpect(jsonPath("$.conversation[0].text").value("추리 소설 좋아해요"))
            .andExpect(jsonPath("$.existing_preferences").doesNotExist())
            .andRespond(withSuccess(envelope("[" + EXTRACTION + "]", false), MediaType.APPLICATION_JSON));

        final List<UserMemoryExtraction> extractions = client.extract(request());

        assertThat(extractions).containsExactly(
            new UserMemoryExtraction(UserMemoryType.TOPIC, "추리 소설을 좋아함", 2, List.of("추리/스릴러"), "추리 소설", List.of(0.054f, -0.25f), 2));
    }

    @Test
    void 뽑은_취향이_없으면_빈_목록을_반환한다() {
        server.expect(requestTo(EXTRACTION_URL)).andRespond(withSuccess(envelope("[]", true), MediaType.APPLICATION_JSON));

        assertThat(client.extract(request())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"type\":\"topic\"→\"type\":\"TOPIC\"", "\"type\":\"topic\"→\"type\":\"genre\"", "\"keyword\":\"추리 소설\",→",
        "\"dim\":2,→", "cv_20261013_a1→cv_other"})
    void 응답_필드가_없거나_계약과_다르면_잘못된_응답으로_변환한다(final String replacement) {
        final String[] parts = replacement.split("→", -1);
        server.expect(requestTo(EXTRACTION_URL))
            .andRespond(withSuccess(envelope("[" + EXTRACTION.replace(parts[0], parts[1]) + "]", false), MediaType.APPLICATION_JSON));

        assertInvalidResponse();
    }

    @Test
    void 뽑을_것이_없다면서_추출_결과를_보내면_잘못된_응답으로_변환한다() {
        server.expect(requestTo(EXTRACTION_URL))
            .andRespond(withSuccess(envelope("[" + EXTRACTION + "]", true), MediaType.APPLICATION_JSON));

        assertInvalidResponse();
    }

    @Test
    void JSON이_아닌_응답은_잘못된_응답으로_변환한다() {
        server.expect(requestTo(EXTRACTION_URL)).andRespond(withSuccess("{\"data\":", MediaType.APPLICATION_JSON));

        assertInvalidResponse();
    }

    @Test
    void AI가_오류를_응답하면_AI_서비스_호출_실패로_변환한다() {
        server.expect(requestTo(EXTRACTION_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.extract(request())).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.AI_SERVICE_FAILURE));
    }

    private void assertInvalidResponse() {
        assertThatThrownBy(() -> client.extract(request())).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode())
                .isEqualTo(ErrorCode.AI_MEMORY_EXTRACTION_INVALID_RESPONSE));
    }

    private AiMemoryExtractionRequest request() {
        return new AiMemoryExtractionRequest(42L, true, CONVERSATION_ID, List.of(new ConversationMessage("user", "추리 소설 좋아해요")));
    }

    private String envelope(final String extractions, final boolean nothingFound) {
        return "{\"message\":\"extract_success\",\"data\":{\"extractions\":" + extractions + ",\"nothing_found\":" + nothingFound + "}}";
    }
}
