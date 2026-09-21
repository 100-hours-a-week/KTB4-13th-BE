package com.book.core.category.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.category.application.port.CategoryRepositoryPort;
import com.book.core.category.application.result.GetCategoriesResult;
import com.book.core.category.application.result.GetCategoryItemResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetCategoriesUseCase {
    private final CategoryRepositoryPort categoryRepository;

    @Transactional(readOnly = true)
    public GetCategoriesResult execute() {
        final var categories = categoryRepository.findActive().stream()
                .map(GetCategoryItemResult::from)
                .toList();
        return new GetCategoriesResult(categories);
    }
}
