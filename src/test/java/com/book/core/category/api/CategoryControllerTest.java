package com.book.core.category.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.category.application.result.GetCategoriesResult;
import com.book.core.category.application.result.GetCategoryItemResult;
import com.book.core.category.application.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@ActiveProfiles("test")
class CategoryControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    CategoryService categoryService;

    @Test
    void 상품_카테고리_목록을_공개_API_응답으로_반환한다() throws Exception {
        when(categoryService.getCategories())
                .thenReturn(GetCategoriesResult.of(List.of(new GetCategoryItemResult(7L, "소설", "도서/소설"))));

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categories[0].id").value(7))
                .andExpect(jsonPath("$.data.categories[0].name").value("소설"))
                .andExpect(jsonPath("$.data.categories[0].path").value("도서/소설"));

        verify(categoryService).getCategories();
    }

    @Test
    void 카테고리가_없으면_빈_목록을_반환한다() throws Exception {
        when(categoryService.getCategories()).thenReturn(GetCategoriesResult.of(List.of()));

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categories").isArray())
                .andExpect(jsonPath("$.data.categories").isEmpty());
    }

    @Test
    void 저장소_오류는_E500으로_응답한다() throws Exception {
        when(categoryService.getCategories()).thenThrow(new IllegalStateException("database detail"));

        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }
}
