package com.book.core.product.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.api.converter.ProductCommandConverter;
import com.book.core.product.api.converter.ProductResultConverter;
import com.book.core.product.application.result.GetProductCouponResult;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import({ProductCommandConverter.class, ProductResultConverter.class})
@ActiveProfiles("test")
class ProductControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService productService;

    @Test
    void 상품_상세를_명세_필드로_응답한다() throws Exception {
        when(productService.getProductDetail(any()))
                .thenReturn(new GetProductDetailResult(
                        20L,
                        "상품명",
                        "thumbnail.jpg",
                        "작가",
                        "출판사",
                        LocalDate.of(2026, 1, 1),
                        new BigDecimal("20000.00"),
                        new BigDecimal("18000.00"),
                        3L,
                        new BigDecimal("4.5"),
                        10,
                        List.of(new GetProductCouponResult(
                                1L,
                                "ACTIVE",
                                "할인 쿠폰",
                                "PERCENT",
                                new BigDecimal("10.00"),
                                new BigDecimal("10000.00"),
                                new BigDecimal("5000.00"),
                                1,
                                0,
                                "2026-12-31T23:59:59"))));

        mvc.perform(get("/api/v1/products/{productId}", 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(20))
                .andExpect(jsonPath("$.data.itemId").doesNotExist())
                .andExpect(jsonPath("$.data.itemName").value("상품명"))
                .andExpect(jsonPath("$.data.thumbnailUrl").value("thumbnail.jpg"))
                .andExpect(jsonPath("$.data.author").value("작가"))
                .andExpect(jsonPath("$.data.publisher").value("출판사"))
                .andExpect(jsonPath("$.data.publishedAt").value("2026-01-01"))
                .andExpect(jsonPath("$.data.salePrice").value(20000.00))
                .andExpect(jsonPath("$.data.discountedPrice").value(18000.00))
                .andExpect(jsonPath("$.data.reviewCount").value(3))
                .andExpect(jsonPath("$.data.reviewRate").value(4.5))
                .andExpect(jsonPath("$.data.stockQuantity").value(10))
                .andExpect(jsonPath("$.data.coupons[0].id").value(1))
                .andExpect(jsonPath("$.data.coupons[0].name").value("할인 쿠폰"));

        verify(productService).getProductDetail(any());
    }

    @Test
    void 존재하지_않는_상품은_E404를_응답한다() throws Exception {
        when(productService.getProductDetail(any())).thenThrow(new CoreException(ErrorCode.PRODUCT_NOT_FOUND));

        mvc.perform(get("/api/v1/products/20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }

    @Test
    void 양수가_아닌_상품_ID는_E400으로_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(get("/api/v1/products/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(productService);
    }

    @Test
    void 숫자가_아닌_상품_ID는_E400으로_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(get("/api/v1/products/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(productService);
    }

    @Test
    void 저장소_오류는_E500으로_응답한다() throws Exception {
        when(productService.getProductDetail(any())).thenThrow(new IllegalStateException("database detail"));

        mvc.perform(get("/api/v1/products/20"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }
}
