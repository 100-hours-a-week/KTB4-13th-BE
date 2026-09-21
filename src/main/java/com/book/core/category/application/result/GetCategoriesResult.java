package com.book.core.category.application.result;

import java.util.List;

public record GetCategoriesResult(List<GetCategoryItemResult> categories) {}
