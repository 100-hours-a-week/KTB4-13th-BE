package com.book.core.product.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.book.core.book.domain.Book;
import com.book.core.product.domain.Product;
import com.book.core.product.application.result.CartChangeProductResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class CartChangeProductRepositoryAdapterTest {
    @Test
    void 한_번_조회하고_상품과_도서_ID순으로_낙관적_버전_검사를_등록한다() {
        final EntityManager entityManager = mock(EntityManager.class);
        final ProductJpaRepository repository = mock(ProductJpaRepository.class);
        final ProductRepositoryAdapter adapter =
            new ProductRepositoryAdapter(repository, mock(ProductQueryRepository.class), entityManager);
        final Book firstBook = new Book(20L, null, null, "첫 책", "작가", null, "출판사", "소설", LocalDate.now(), null);
        final Book secondBook = new Book(10L, null, null, "둘째 책", "작가", null, "출판사", "소설", LocalDate.now(), null);
        final Product first = new Product(1L, firstBook, "상품", null, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, 0);
        final Product second = new Product(2L, secondBook, "상품", null, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, 5);
        second.delete();
        final List<Long> ids = List.of(2L, 1L, 2L, 999L);
        when(repository.findByIdInWithBook(ids)).thenReturn(List.of(second, first));
        final Map<Long, CartChangeProductResult> result = adapter.findByIdsForChange(ids);
        assertThat(result).containsOnlyKeys(1L, 2L);
        assertThat(result.get(1L).stockQuantity()).isZero();
        assertThat(result.get(1L).active()).isTrue();
        assertThat(result.get(2L).active()).isFalse();
        final InOrder order = inOrder(repository, entityManager);
        order.verify(repository).findByIdInWithBook(ids);
        order.verify(entityManager).lock(first, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        order.verify(entityManager).lock(second, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        order.verify(entityManager).lock(secondBook, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        order.verify(entityManager).lock(firstBook, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        order.verifyNoMoreInteractions();
    }
}
