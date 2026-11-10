package com.book.core.library.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.library.application.command.ScanShelfPhotoCommand;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.application.result.ShelfScanResult.Item;
import com.book.core.library.application.result.ShelfScanResult.Spine;
import com.book.core.library.application.usecase.CreateShelfPhotoUploadUseCase;
import com.book.core.library.application.usecase.DeleteLibraryBooksUseCase;
import com.book.core.library.application.usecase.GetLibraryBooksUseCase;
import com.book.core.library.application.usecase.ScanShelfPhotoUseCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LibraryServiceTest {
    private static final ScanShelfPhotoCommand SCAN = new ScanShelfPhotoCommand(42L, "shelf-photos/42/photo.jpg");

    private final ScanShelfPhotoUseCase scanShelfPhotoUseCase = mock(ScanShelfPhotoUseCase.class);
    private final GetAiBookIdentifiersUseCase getAiBookIdentifiersUseCase = mock(GetAiBookIdentifiersUseCase.class);
    private final LibraryService service = new LibraryService(mock(GetLibraryBooksUseCase.class), mock(DeleteLibraryBooksUseCase.class),
        mock(CreateShelfPhotoUploadUseCase.class), scanShelfPhotoUseCase, getAiBookIdentifiersUseCase);

    @Test
    void 사진에서_찾은_책은_ISBN으로_찾은_BE_도서_ID를_붙이고_연결되지_않은_책과_못_찾은_책은_bookId_없이_전달한다() {
        final List<Spine> spines = List.of(new Spine(1, List.of(List.of(0.1, 0.2))));
        when(scanShelfPhotoUseCase.execute(SCAN))
            .thenReturn(new ShelfScanResult("shelf", List.of(new Item("FOUND", "책1", book("9788900000011"), spines),
                new Item("FOUND", "책2", book("9788900000028"), List.of()), new Item("NOT_FOUND", "읽은 글자", null, List.of()))));
        when(getAiBookIdentifiersUseCase.findBookIds(List.of("9788900000011", "9788900000028"))).thenReturn(Map.of("9788900000011", 101L));

        final ShelfScanResult result = service.scanPhoto(SCAN);

        assertThat(result.photoType()).isEqualTo("shelf");
        assertThat(result.items()).extracting(item -> item.book() == null ? null : item.book().bookId()).containsExactly(101L, null, null);
        assertThat(result.items().getFirst().book().isbn()).isEqualTo("9788900000011");
        assertThat(result.items().getFirst().spines()).isEqualTo(spines);
        assertThat(result.items().getLast()).isEqualTo(new Item("NOT_FOUND", "읽은 글자", null, List.of()));
    }

    @Test
    void 찾은_책이_없으면_도서_ID를_조회하지_않는다() {
        when(scanShelfPhotoUseCase.execute(SCAN)).thenReturn(new ShelfScanResult("none", List.of()));

        assertThat(service.scanPhoto(SCAN).items()).isEmpty();
        verifyNoInteractions(getAiBookIdentifiersUseCase);
    }

    private static ShelfScanResult.Book book(final String isbn) {
        return new ShelfScanResult.Book(null, isbn, "제목", "저자", "출판사", 2020, null);
    }
}
