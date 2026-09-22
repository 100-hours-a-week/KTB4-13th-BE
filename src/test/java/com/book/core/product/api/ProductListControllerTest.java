package com.book.core.product.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.product.api.converter.ProductCommandConverter;
import com.book.core.product.api.converter.ProductResultConverter;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.result.GetProductItemResult;
import com.book.core.product.application.result.GetProductsResult;
import com.book.core.product.application.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductListController.class)
@Import({ProductCommandConverter.class, ProductResultConverter.class})
@ActiveProfiles("test")
class ProductListControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService productService;

    @Test
    void 상품_목록을_명세_필드로_응답한다() throws Exception {
        when(productService.getProducts(any()))
                .thenReturn(GetProductsResult.of(
                        List.of(new GetProductItemResult(
                                101L,
                                "상품명",
                                "thumbnail.jpg",
                                "작가",
                                new BigDecimal("20000.00"),
                                new BigDecimal("18000.00"),
                                0L,
                                0L,
                                BigDecimal.ZERO)),
                        "101"));

        mvc.perform(get("/api/v1/items")
                        .queryParam("categoryId", "7")
                        .queryParam("sort", "createdAt")
                        .queryParam("cursor", "102")
                        .queryParam("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].itemId").value(101))
                .andExpect(jsonPath("$.data.items[0].itemName").value("상품명"))
                .andExpect(jsonPath("$.data.items[0].thumbnailUrl").value("thumbnail.jpg"))
                .andExpect(jsonPath("$.data.items[0].author").value("작가"))
                .andExpect(jsonPath("$.data.items[0].salePrice").value(20000.00))
                .andExpect(jsonPath("$.data.items[0].discountedPrice").value(18000.00))
                .andExpect(jsonPath("$.data.items[0].orderCount").value(0))
                .andExpect(jsonPath("$.data.items[0].reviewCount").value(0))
                .andExpect(jsonPath("$.data.items[0].reviewRate").value(0))
                .andExpect(jsonPath("$.data.nextCursor").value("101"));

        final var commandCaptor = ArgumentCaptor.forClass(GetProductsCommand.class);
        verify(productService).getProducts(commandCaptor.capture());
        assertThat(commandCaptor.getValue().categoryId()).isEqualTo(7L);
        assertThat(commandCaptor.getValue().sort()).isEqualTo("createdAt");
        assertThat(commandCaptor.getValue().cursor()).isEqualTo(102L);
        assertThat(commandCaptor.getValue().limit()).isEqualTo(1);
    }

    @Test
    void 상품이_없으면_빈_목록을_응답한다() throws Exception {
        when(productService.getProducts(any())).thenReturn(GetProductsResult.of(List.of(), null));

        mvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void 양수가_아닌_카테고리와_페이지_크기는_E400이다() throws Exception {
        mvc.perform(get("/api/v1/items").queryParam("categoryId", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/items").queryParam("limit", "0")).andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void 숫자가_아닌_커서는_E400이다() throws Exception {
        mvc.perform(get("/api/v1/items").queryParam("cursor", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(productService);
    }

    @Test
    void 저장소_오류는_E500으로_응답한다() throws Exception {
        when(productService.getProducts(any())).thenThrow(new IllegalStateException("database list"));

        mvc.perform(get("/api/v1/items"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }
}
