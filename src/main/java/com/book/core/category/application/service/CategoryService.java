package com.book.core.category.application.service;

import com.book.core.category.application.result.GetCategoriesResult;
import com.book.core.category.application.usecase.GetCategoriesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final GetCategoriesUseCase getCategoriesUseCase;

    public GetCategoriesResult getCategories() {
        return getCategoriesUseCase.execute();
    }
}
