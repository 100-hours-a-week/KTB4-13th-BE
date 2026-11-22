package com.book.core.book.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.book.application.port.NewBookCollector;
import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.resilience.annotation.EnableResilientMethods;

class NewBookCollectorAdapterRetryTest {
    private static final String ISBN = "9788983921987";
    private final NationalLibraryClient nationalLibrary = mock(NationalLibraryClient.class);
    private final JeongboNaruClient jeongboNaru = mock(JeongboNaruClient.class);
    private AnnotationConfigApplicationContext context;
    private NewBookCollector collector;

    @BeforeEach
    void startContext() {
        context = new AnnotationConfigApplicationContext();
        context.register(RetryConfiguration.class);
        context.registerBean(NewBookCollectorAdapter.class, () -> new NewBookCollectorAdapter(nationalLibrary, jeongboNaru));
        context.refresh();
        collector = context.getBean(NewBookCollector.class);
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void 일시_실패는_두_번까지_다시_시도한다() {
        final var book = new JeongboNaruResponse.Book(ISBN, "신간", "저자", "출판사", "813.7", null, null);
        when(jeongboNaru.findBookDetail(ISBN)).thenThrow(new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY))
            .thenReturn(new JeongboNaruResponse(new JeongboNaruResponse.Body(null, null, List.of(new JeongboNaruResponse.Detail(book)))));

        assertThat(collector.findDetail(ISBN)).isPresent();
        verify(jeongboNaru, times(2)).findBookDetail(ISBN);
    }

    @Test
    void 일시_실패가_계속되면_세_번_호출_후_예외를_던진다() {
        when(jeongboNaru.findBookDetail(ISBN)).thenThrow(new NewBookCollectorException(NewBookCollectorFailureReason.TEMPORARY));

        assertThatThrownBy(() -> collector.findDetail(ISBN)).isInstanceOf(NewBookCollectorException.class);
        verify(jeongboNaru, times(3)).findBookDetail(ISBN);
    }

    @Test
    void 요청_거부와_호출_제한은_다시_시도하지_않는다() {
        when(jeongboNaru.findBookDetail(ISBN)).thenThrow(new NewBookCollectorException(NewBookCollectorFailureReason.REJECTED, "authErr"),
            new NewBookCollectorException(NewBookCollectorFailureReason.RATE_LIMIT));

        assertThatThrownBy(() -> collector.findDetail(ISBN)).isInstanceOf(NewBookCollectorException.class);
        assertThatThrownBy(() -> collector.findDetail(ISBN)).isInstanceOf(NewBookCollectorException.class);
        verify(jeongboNaru, times(2)).findBookDetail(ISBN);
    }

    @EnableResilientMethods(proxyTargetClass = true)
    static class RetryConfiguration {
    }
}
