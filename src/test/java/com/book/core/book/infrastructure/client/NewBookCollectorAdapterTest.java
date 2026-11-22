package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookCandidate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class NewBookCollectorAdapterTest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 13);
    private static final LocalDate THROUGH = LocalDate.of(2026, 11, 11);
    private static final NewBookCandidate FIRST = new NewBookCandidate("9788983921987", LocalDate.of(2026, 11, 1));
    private static final NewBookCandidate SECOND = new NewBookCandidate("9788972756194", LocalDate.of(2026, 11, 2));

    @Mock
    private NationalLibraryClient nationalLibrary;
    @Mock
    private JeongboNaruClient jeongboNaru;
    @InjectMocks
    private NewBookCollectorAdapter collector;

    @Test
    void 국중도_목록을_마지막_페이지까지_모은다() {
        when(nationalLibrary.searchPaperBooks(FROM, THROUGH, 1, 100)).thenReturn(page("101", FIRST));
        when(nationalLibrary.searchPaperBooks(FROM, THROUGH, 2, 100)).thenReturn(page("101", SECOND));

        assertThat(collector.collectCandidates(FROM, THROUGH)).containsExactly(FIRST, SECOND);
        verifyNoInteractions(jeongboNaru);
    }

    @Test
    void 국중도_후보는_실제_출간일을_우선하고_날짜가_없으면_뺀다() {
        when(nationalLibrary.searchPaperBooks(FROM, THROUGH, 1, 100)).thenReturn(new NationalLibraryResponse(null, null, "3",
            List.of(new NationalLibraryResponse.Book(FIRST.isbn13(), "", "20261111"),
                new NationalLibraryResponse.Book(SECOND.isbn13(), "20261020", "20261101"),
                new NationalLibraryResponse.Book("9788950983529", "", ""))));

        assertThat(collector.collectCandidates(FROM, THROUGH)).containsExactly(
            new NewBookCandidate(FIRST.isbn13(), LocalDate.of(2026, 11, 11)),
            new NewBookCandidate(SECOND.isbn13(), LocalDate.of(2026, 10, 20)));
    }

    @Test
    void 국중도_목록_중간_페이지가_오류면_외부_코드를_담아_실패한다() {
        when(nationalLibrary.searchPaperBooks(FROM, THROUGH, 1, 100)).thenReturn(page("101", FIRST));
        when(nationalLibrary.searchPaperBooks(FROM, THROUGH, 2, 100))
            .thenReturn(new NationalLibraryResponse("ERROR", "011", null, null));

        assertThatThrownBy(() -> collector.collectCandidates(FROM, THROUGH)).isInstanceOfSatisfying(NewBookCollectorException.class,
            (final var error) -> {
                assertThat(error.reason()).isEqualTo(NewBookCollectorFailureReason.REJECTED);
                assertThat(error.externalCode()).isEqualTo("011");
            });
    }

    @Test
    void 상세는_정보나루에서_가져오고_값의_앞뒤_공백을_없앤다() {
        final var book = new JeongboNaruResponse.Book(FIRST.isbn13(), " 신간 ", "저자", "출판사", "813.7", " ", " https://image ");
        when(jeongboNaru.findBookDetail(FIRST.isbn13())).thenReturn(detail(book));

        assertThat(collector.findDetail(FIRST.isbn13())).contains(new BookDetail("신간", "저자", null, "출판사", "813.7", "https://image"));
        verify(jeongboNaru).findBookDetail(FIRST.isbn13());
        verifyNoInteractions(nationalLibrary);
    }

    @Test
    void 정보나루_분류번호가_비어_있으면_상세_없음으로_본다() {
        final var book = new JeongboNaruResponse.Book(FIRST.isbn13(), "신간", "저자", "출판사", "", null, null);
        when(jeongboNaru.findBookDetail(FIRST.isbn13())).thenReturn(detail(book));

        assertThat(collector.findDetail(FIRST.isbn13())).isEmpty();
    }

    @Test
    void 정보나루에_없는_ISBN_응답은_상세_없음으로_본다() {
        when(jeongboNaru.findBookDetail(FIRST.isbn13()))
            .thenReturn(new JeongboNaruResponse(new JeongboNaruResponse.Body("isbnMpngErr", "ISBN에 해당하는 도서가 없습니다.", null)));

        assertThat(collector.findDetail(FIRST.isbn13())).isEmpty();
    }

    @Test
    void 정보나루_오류_응답은_외부_코드를_담아_요청_거부로_분류한다() {
        when(jeongboNaru.findBookDetail(FIRST.isbn13()))
            .thenReturn(new JeongboNaruResponse(new JeongboNaruResponse.Body("authErr", "인증정보가 일치하지 않습니다.", null)));

        assertThatThrownBy(() -> collector.findDetail(FIRST.isbn13())).isInstanceOfSatisfying(NewBookCollectorException.class,
            (final var error) -> {
                assertThat(error.reason()).isEqualTo(NewBookCollectorFailureReason.REJECTED);
                assertThat(error.externalCode()).isEqualTo("authErr");
            });
    }

    @Test
    void 통신_오류는_일시_오류로_분류한다() {
        when(jeongboNaru.findBookDetail(FIRST.isbn13())).thenThrow(new ResourceAccessException("timeout"));

        assertThatThrownBy(() -> collector.findDetail(FIRST.isbn13())).isInstanceOfSatisfying(NewBookCollectorException.class,
            (final var error) -> {
                assertThat(error.reason()).isEqualTo(NewBookCollectorFailureReason.TEMPORARY);
                assertThat(error.getCause()).isNull();
            });
    }

    private static JeongboNaruResponse detail(final JeongboNaruResponse.Book book) {
        return new JeongboNaruResponse(new JeongboNaruResponse.Body(null, null, List.of(new JeongboNaruResponse.Detail(book))));
    }

    private static NationalLibraryResponse page(final String totalCount, final NewBookCandidate candidate) {
        final String date = candidate.publishedAt().format(DateTimeFormatter.BASIC_ISO_DATE);
        return new NationalLibraryResponse(null, null, totalCount, List.of(new NationalLibraryResponse.Book(candidate.isbn13(), "", date)));
    }
}
