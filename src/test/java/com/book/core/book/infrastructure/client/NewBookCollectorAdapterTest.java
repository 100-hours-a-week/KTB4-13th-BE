package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.result.BookDetail;
import com.book.core.book.application.result.NewBookCandidate;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void 목록_조회가_중간에_실패하면_예외를_그대로_전달한다() {
        when(nationalLibrary.searchPaperBooks(FROM, THROUGH, 1, 100)).thenReturn(page("101", FIRST));
        when(nationalLibrary.searchPaperBooks(FROM, THROUGH, 2, 100))
            .thenReturn(new NationalLibraryResponse("ERROR", "011", null, null));

        assertThatThrownBy(() -> collector.collectCandidates(FROM, THROUGH)).isInstanceOfSatisfying(NewBookCollectorException.class,
            (final var error) -> assertThat(error.externalCode()).isEqualTo("011"));
    }

    @Test
    void 상세는_정보나루에서_가져온다() {
        final var book = new JeongboNaruResponse.Book(FIRST.isbn13(), " 신간 ", "저자", "출판사", "813.7", " ", "https://image");
        when(jeongboNaru.findBookDetail(FIRST.isbn13())).thenReturn(
            new JeongboNaruResponse(new JeongboNaruResponse.Body(null, null, List.of(new JeongboNaruResponse.Detail(book)))));

        assertThat(collector.collectDetail(FIRST.isbn13()))
            .contains(new BookDetail("신간", "저자", null, "출판사", "813.7", "https://image"));
        verify(jeongboNaru).findBookDetail(FIRST.isbn13());
        verifyNoInteractions(nationalLibrary);
    }

    private static NationalLibraryResponse page(final String totalCount, final NewBookCandidate candidate) {
        final String date = candidate.publishedAt().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        return new NationalLibraryResponse(null, null, totalCount, List.of(new NationalLibraryResponse.Book(candidate.isbn13(), "", date)));
    }
}
