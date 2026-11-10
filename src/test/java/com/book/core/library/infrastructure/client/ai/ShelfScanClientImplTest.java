package com.book.core.library.infrastructure.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.application.result.ShelfScanResult.Book;
import com.book.core.library.application.result.ShelfScanResult.Item;
import com.book.core.library.application.result.ShelfScanResult.Spine;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ShelfScanClientImplTest {
    private static final String BASE_URL = "http://ai.internal:8000";
    private static final String SCAN_URL = BASE_URL + "/shelf/scans";
    private static final String IMAGE_REF = "https://book-photos.s3.ap-northeast-2.amazonaws.com/shelf-photos/42/a.jpg?X-Amz-Signature=sig";

    private final RestClient.Builder builder =
        RestClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer service-token");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final ShelfScanClientImpl client = new ShelfScanClientImpl(builder.build());

    @Test
    void 서비스_토큰과_회원_ID_사진_주소를_보내고_찾은_책과_못_찾은_책을_AI의_도서_ID_없이_변환한다() {
        server.expect(requestTo(SCAN_URL)).andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token")).andExpect(jsonPath("$.user_id").value(42))
            .andExpect(jsonPath("$.image_ref").value(IMAGE_REF)).andRespond(withSuccess("""
                {"message":"shelf_scan_success","data":{"photo_type":"shelf","items":[
                  {"state":"FOUND","read_text":"루비 프로그래밍 언어","book":{"book_id":1234,"isbn":"9788970507538","title":"루비 프로그래밍 언어",
                   "author":"Peter Cooper 외","publisher":"생능출판사","year":2012,"cover_url":null},
                   "spines":[{"order":5,"polygon":[[0.27,0.24],[0.29,0.81]]}]},
                  {"state":"NOT_FOUND","read_text":"COVERT JAVA","book":null,"spines":[]}]}}
                """, MediaType.APPLICATION_JSON));

        final ShelfScanResult scan = client.scan(42L, IMAGE_REF);

        server.verify();
        assertThat(scan).isEqualTo(new ShelfScanResult("shelf",
            List.of(
                new Item("FOUND", "루비 프로그래밍 언어", new Book(null, "9788970507538", "루비 프로그래밍 언어", "Peter Cooper 외", "생능출판사", 2012, null),
                    List.of(new Spine(5, List.of(List.of(0.27, 0.24), List.of(0.29, 0.81))))),
                new Item("NOT_FOUND", "COVERT JAVA", null, List.of()))));
    }

    @Test
    void 책을_하나도_못_찾은_200_응답은_빈_목록으로_변환한다() {
        server.expect(requestTo(SCAN_URL)).andRespond(withSuccess("""
            {"message":"shelf_scan_success","data":{"photo_type":"none","items":[]}}
            """, MediaType.APPLICATION_JSON));

        assertThat(client.scan(42L, IMAGE_REF)).isEqualTo(new ShelfScanResult("none", List.of()));
    }

    @ParameterizedTest
    @CsvSource({"400,AI_SHELF_SCAN_FAILURE", "404,SHELF_PHOTO_NOT_FOUND", "413,SHELF_PHOTO_TOO_LARGE", "422,SHELF_PHOTO_UNREADABLE",
        "500,AI_SHELF_SCAN_FAILURE", "503,AI_SHELF_SCAN_UNAVAILABLE", "504,AI_SHELF_SCAN_TIMEOUT"})
    void AI_오류_상태를_사진_인식_오류로_변환한다(final int status, final ErrorCode expected) {
        server.expect(requestTo(SCAN_URL)).andRespond(withStatus(HttpStatus.valueOf(status)));

        assertError(expected);
    }

    @Test
    void 응답_대기_시간을_넘기면_사진_인식_시간_초과로_변환한다() {
        server.expect(requestTo(SCAN_URL)).andRespond(withException(new SocketTimeoutException("read timed out")));

        assertError(ErrorCode.AI_SHELF_SCAN_TIMEOUT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"message\":\"other\",\"data\":{\"photo_type\":\"cover\",\"items\":[]}}",
        "{\"message\":\"shelf_scan_success\",\"data\":null}", "{\"message\":\"shelf_scan_success\",\"data\":{\"photo_type\":\"cover\"}}",
        "{\"message\":\"shelf_scan_success\",\"data\":{\"photo_type\":\"cover\",\"items\":[{\"state\":\"MAYBE\",\"spines\":[]}]}}",
        "{\"message\":\"shelf_scan_success\",\"data\":{\"photo_type\":\"cover\","
            + "\"items\":[{\"state\":\"FOUND\",\"book\":null,\"spines\":[]}]}}",
        "{\"message\":\"shelf_scan_success\",\"data\":{\"photo_type\":\"cover\",\"items\":[null]}}", "not-json"})
    void 계약과_다른_응답은_사진_인식_실패로_변환한다(final String body) {
        server.expect(requestTo(SCAN_URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertError(ErrorCode.AI_SHELF_SCAN_FAILURE);
    }

    private void assertError(final ErrorCode expected) {
        assertThatThrownBy(() -> client.scan(42L, IMAGE_REF)).isInstanceOfSatisfying(CoreException.class, exception -> {
            assertThat(exception.errorCode()).isEqualTo(expected);
            assertThat(exception.getMessage()).doesNotContain("X-Amz-Signature");
        });
    }
}
