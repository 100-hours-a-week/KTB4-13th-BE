package com.book.core.category.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.category.application.port.CategoryRepositoryPort;
import com.book.core.category.application.result.GetCategoryItemResult;
import com.book.core.category.application.usecase.GetCategoriesUseCase;
import com.book.core.category.domain.Category;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetCategoriesUseCaseTest {
    private final FakeCategoryRepository categoryRepository = new FakeCategoryRepository();
    private final GetCategoriesUseCase useCase = new GetCategoriesUseCase(categoryRepository);

    @Test
    void 활성_카테고리_목록을_조회_결과로_변환한다() {
        categoryRepository.categories =
                List.of(new Category(7L, "소설", "도서/소설", null), new Category(8L, "에세이", "도서/에세이", null));

        final var result = useCase.execute();

        assertThat(categoryRepository.called).isTrue();
        assertThat(result.categories())
                .extracting(GetCategoryItemResult::id, GetCategoryItemResult::name, GetCategoryItemResult::path)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(7L, "소설", "도서/소설"),
                        org.assertj.core.groups.Tuple.tuple(8L, "에세이", "도서/에세이"));
    }

    @Test
    void 카테고리가_없으면_빈_목록을_반환한다() {
        final var result = useCase.execute();

        assertThat(result.categories()).isEmpty();
    }

    private static final class FakeCategoryRepository implements CategoryRepositoryPort {
        private List<Category> categories = List.of();
        private boolean called;

        @Override
        public List<Category> findActive() {
            called = true;
            return categories;
        }
    }
}
