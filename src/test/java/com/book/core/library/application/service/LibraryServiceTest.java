package com.book.core.library.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.library.application.command.AddLibraryBooksCommand;
import com.book.core.library.application.usecase.AddLibraryBooksUseCase;
import com.book.core.library.application.usecase.CreateShelfPhotoUploadUseCase;
import com.book.core.library.application.usecase.DeleteLibraryBooksUseCase;
import com.book.core.library.application.usecase.GetLibraryBooksUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.book.core.book.application.usecase.GetAiBookIdentifiersUseCase;
import com.book.core.library.application.command.ScanShelfPhotoCommand;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.application.result.ShelfScanResult.Item;
import com.book.core.library.application.result.ShelfScanResult.Spine;
import com.book.core.library.application.usecase.ScanShelfPhotoUseCase;
import java.util.Map;

class LibraryServiceTest {
    private static final ScanShelfPhotoCommand SCAN = new ScanShelfPhotoCommand(42L, "shelf-photos/42/photo.jpg");
    private final ScanShelfPhotoUseCase scanShelfPhotoUseCase = mock(ScanShelfPhotoUseCase.class);
    private final GetAiBookIdentifiersUseCase getAiBookIdentifiersUseCase = mock(GetAiBookIdentifiersUseCase.class);
    private final AddLibraryBooksUseCase addLibraryBooksUseCase = mock(AddLibraryBooksUseCase.class);
    private final GetBooksUseCase getBooksUseCase = mock(GetBooksUseCase.class);
    private final LibraryService service = new LibraryService(mock(GetLibraryBooksUseCase.class), addLibraryBooksUseCase, getBooksUseCase,
        mock(DeleteLibraryBooksUseCase.class), mock(CreateShelfPhotoUploadUseCase.class), scanShelfPhotoUseCase,
        getAiBookIdentifiersUseCase);

    @Test
    void 고른_책이_모두_있으면_중복을_합쳐_확인한_뒤_기존_추가_UseCase로_담는다() {
        final AddLibraryBooksCommand command = new AddLibraryBooksCommand(42L, List.of(1L, 2L, 1L));
        when(getBooksUseCase.execute(List.of(1L, 2L))).thenReturn(List.of(mock(Book.class), mock(Book.class)));

        service.addBooks(command);

        verify(addLibraryBooksUseCase).execute(command);
    }

    @Test
    void 존재하지_않는_책이_하나라도_있으면_아무것도_담지_않고_404로_거부한다() {
        when(getBooksUseCase.execute(List.of(1L, 999L))).thenReturn(List.of(mock(Book.class)));

        assertThatThrownBy(() -> service.addBooks(new AddLibraryBooksCommand(42L, List.of(1L, 999L)))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.LIBRARY_BOOK_NOT_FOUND));
        verifyNoInteractions(addLibraryBooksUseCase);
    }

    @Test
    void 동시_담기로_UNIQUE_제약이_충돌하면_한_번만_다시_실행하고_반복되면_그대로_전파한다() {
        final AddLibraryBooksCommand command = new AddLibraryBooksCommand(42L, List.of(1L));
        when(getBooksUseCase.execute(List.of(1L))).thenReturn(List.of(mock(Book.class)));
        doThrow(new DuplicateKeyException("uk")).doNothing().when(addLibraryBooksUseCase).execute(command);

        service.addBooks(command);
        verify(addLibraryBooksUseCase, times(2)).execute(command);

        doThrow(new DuplicateKeyException("uk")).when(addLibraryBooksUseCase).execute(command);
        assertThatThrownBy(() -> service.addBooks(command)).isInstanceOf(DuplicateKeyException.class);
        verify(addLibraryBooksUseCase, times(4)).execute(command);
    }

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
