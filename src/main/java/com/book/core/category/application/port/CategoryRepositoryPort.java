package com.book.core.category.application.port;

import com.book.core.category.domain.Category;
import java.util.List;

public interface CategoryRepositoryPort {
    List<Category> findActive();
}
