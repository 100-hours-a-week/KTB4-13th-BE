package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookCandidate;
import com.book.core.book.domain.Isbn13;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
class NewBookCollectorAdapter implements NewBookCollector {
    private static final int PAGE_SIZE = 100;
    private static final String MISSING_ISBN_ERROR_CODE = "isbnMpngErr";
    private final NationalLibraryClient nationalLibrary;
    private final JeongboNaruClient jeongboNaru;

    @Override
    public List<NewBookCandidate> collectCandidates(final LocalDate from, final LocalDate through) {
        final var candidates = new ArrayList<NewBookCandidate>();
        int calls = 0;
        try {
            for (int pageNumber = 1;; pageNumber++) {
                calls = pageNumber;
                final int page = pageNumber;
                final var response = call(() -> nationalLibrary.searchPaperBooks(from, through, page, PAGE_SIZE));
                final int totalCount = totalCount(response, page);
                if (response.docs() != null) {
                    response.docs().stream().map(NewBookCollectorAdapter::toCandidate).filter(Objects::nonNull)
                        .forEachOrdered(candidates::add);
                }
                if ((long) page * PAGE_SIZE >= totalCount) {
                    break;
                }
            }
        } finally {
            final int candidateCount = candidates.size();
            log.atInfo().addKeyValue("listCallCount", calls).addKeyValue("candidateCount", candidateCount)
                .log("[BOOK] 국중도 신간 후보 수집이 종료됐습니다. listCallCount={}, candidateCount={}", calls, candidateCount);
        }
        return List.copyOf(candidates);
    }

    @Override
    @Retryable(predicate = TemporaryFailureRetryPredicate.class, maxRetries = 2, delay = 500)
    public Optional<BookDetail> collectDetail(final String isbn13) {
        final var body = call(() -> jeongboNaru.findBookDetail(isbn13)).response();
        if (body == null) {
            throw new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY);
        }
        if (body.error() != null || body.errCode() != null) {
            if (MISSING_ISBN_ERROR_CODE.equals(body.errCode())) {
                return Optional.empty();
            }
            throw new NewBookCollectorException(NewBookCollectorFailureReason.REJECTED, body.errCode());
        }
        if (body.detail() == null) {
            return Optional.empty();
        }
        return body.detail().stream().map(JeongboNaruResponse.Detail::book).filter(Objects::nonNull)
            .filter((final var book) -> Isbn13.normalize(book.isbn13()).filter(isbn13::equals).isPresent())
            .map(NewBookCollectorAdapter::toBookDetail).flatMap(Optional::stream).findFirst();
    }

    private static int totalCount(final NationalLibraryResponse response, final int pageNumber) {
        if ("ERROR".equals(response.result())) {
            throw new NewBookCollectorException(NewBookCollectorFailureReason.REJECTED, response.errorCode());
        }
        final int totalCount;
        try {
            totalCount = Integer.parseInt(Objects.requireNonNull(response.totalCount()).strip());
        } catch (final NullPointerException | NumberFormatException exception) {
            throw new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY);
        }
        final boolean noDocs = response.docs() == null || response.docs().isEmpty();
        if (totalCount < 0 || (totalCount > (long) (pageNumber - 1) * PAGE_SIZE && noDocs)) {
            throw new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY);
        }
        return totalCount;
    }

    private static NewBookCandidate toCandidate(final NationalLibraryResponse.Book book) {
        LocalDate publishedAt = date(book.realPublishDate());
        if (publishedAt == null) {
            publishedAt = date(book.publishPredate());
        }
        if (book.isbn13() == null || publishedAt == null) {
            return null;
        }
        return new NewBookCandidate(book.isbn13(), publishedAt);
    }

    private static Optional<BookDetail> toBookDetail(final JeongboNaruResponse.Book book) {
        if (!required(book.bookname()) || !required(book.authors()) || !required(book.publisher()) || !required(book.classNo())) {
            return Optional.empty();
        }
        String cover = null;
        if (required(book.bookImageUrl())) {
            cover = book.bookImageUrl().strip();
        }
        String description = null;
        if (book.description() != null && !book.description().isBlank()
            && book.description().getBytes(StandardCharsets.UTF_8).length <= 65535) {
            description = book.description();
        }
        return Optional.of(new BookDetail(book.bookname().strip(), book.authors().strip(), description, book.publisher().strip(),
            book.classNo().strip(), cover));
    }

    private static LocalDate date(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip(), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (final DateTimeParseException exception) {
            return null;
        }
    }

    private static boolean required(final String value) {
        return value != null && !value.isBlank() && value.strip().length() <= 255;
    }

    private static <T> T call(final Supplier<T> request) {
        final T response;
        try {
            response = request.get();
        } catch (final RestClientException exception) {
            throw new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY);
        }
        if (response == null) {
            throw new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY);
        }
        return response;
    }
}
