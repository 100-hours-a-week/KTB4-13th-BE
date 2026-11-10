package com.book.core.library.infrastructure.client.ai;

import com.book.common.config.ai.AiRestClientConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.library.application.port.ShelfScanClient;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.application.result.ShelfScanResult.Book;
import com.book.core.library.application.result.ShelfScanResult.Item;
import com.book.core.library.application.result.ShelfScanResult.Spine;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** image_ref에는 서명된 사진 주소가 들어 있어 요청 본문과 예외 메시지를 로그에 남기지 않습니다. */
@Component
class ShelfScanClientImpl implements ShelfScanClient {
    private static final String SCAN_PATH = "/shelf/scans";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String SUCCESS_MESSAGE = "shelf_scan_success";
    private static final String FOUND = "FOUND";
    private static final Set<String> STATES = Set.of(FOUND, "NOT_FOUND");
    private static final Set<String> PHOTO_TYPES = Set.of("cover", "shelf", "none");

    private final RestClient restClient;

    ShelfScanClientImpl(@Qualifier(AiRestClientConfig.SHELF_SCAN_AI_REST_CLIENT) final RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ShelfScanResult scan(final Long userId, final String imageRef) {
        final AiShelfScanEnvelope envelope;
        try {
            envelope =
                restClient.post().uri(SCAN_PATH).header(REQUEST_ID_HEADER, currentRequestId()).contentType(MediaType.APPLICATION_JSON)
                    .body(new AiShelfScanRequest(userId, imageRef)).retrieve().body(AiShelfScanEnvelope.class);
        } catch (final HttpStatusCodeException exception) {
            throw new CoreException(errorCodeOf(exception.getStatusCode().value()), exception);
        } catch (final ResourceAccessException exception) {
            throw new CoreException(ErrorCode.AI_SHELF_SCAN_TIMEOUT, exception);
        } catch (final RestClientException exception) {
            throw new CoreException(ErrorCode.AI_SHELF_SCAN_FAILURE, exception);
        }
        if (!isValid(envelope)) {
            throw new CoreException(ErrorCode.AI_SHELF_SCAN_FAILURE);
        }
        return new ShelfScanResult(envelope.data().photoType(), envelope.data().items().stream().map(ShelfScanClientImpl::toItem).toList());
    }

    /** 400은 BE가 보낸 사진 주소를 AI가 허용하지 않은 설정 문제라 사용자 오류가 아닌 연동 실패로 다룹니다. */
    private static ErrorCode errorCodeOf(final int status) {
        return switch (status) {
            case 404 -> ErrorCode.SHELF_PHOTO_NOT_FOUND;
            case 413 -> ErrorCode.SHELF_PHOTO_TOO_LARGE;
            case 422 -> ErrorCode.SHELF_PHOTO_UNREADABLE;
            case 503 -> ErrorCode.AI_SHELF_SCAN_UNAVAILABLE;
            case 504 -> ErrorCode.AI_SHELF_SCAN_TIMEOUT;
            default -> ErrorCode.AI_SHELF_SCAN_FAILURE;
        };
    }

    private static boolean isValid(final AiShelfScanEnvelope envelope) {
        return envelope != null && SUCCESS_MESSAGE.equals(envelope.message()) && envelope.data() != null
            && isOneOf(PHOTO_TYPES, envelope.data().photoType()) && envelope.data().items() != null
            && envelope.data().items().stream().allMatch(ShelfScanClientImpl::isValid);
    }

    private static boolean isValid(final AiShelfScanEnvelope.Item item) {
        return item != null && isOneOf(STATES, item.state()) && (FOUND.equals(item.state()) == (item.book() != null))
            && (item.spines() == null || item.spines().stream().allMatch(Objects::nonNull));
    }

    /** Set.of는 null 조회에서 예외를 던지므로, 빠진 값은 계약 위반으로 판정해 연동 실패로 응답합니다. */
    private static boolean isOneOf(final Set<String> allowed, final String value) {
        return value != null && allowed.contains(value);
    }

    private static Item toItem(final AiShelfScanEnvelope.Item item) {
        final AiShelfScanEnvelope.Book book = item.book();
        return new Item(item.state(), item.readText(),
            book == null ? null : new Book(null, book.isbn(), book.title(), book.author(), book.publisher(), book.year(), book.coverUrl()),
            item.spines() == null ? List.of()
                : item.spines().stream().map((final AiShelfScanEnvelope.Spine spine) -> new Spine(spine.order(), spine.polygon()))
                    .toList());
    }

    private String currentRequestId() {
        final String requestId = MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
