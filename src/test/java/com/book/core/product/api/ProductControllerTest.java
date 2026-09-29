package com.book.core.product.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.api.config.SwaggerConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.product.api.converter.ProductCommandConverter;
import com.book.core.product.api.converter.ProductResultConverter;
import com.book.core.product.application.result.GetProductCouponResult;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.command.ProductListSort;
import com.book.core.product.application.result.GetProductItemResult;
import com.book.core.product.application.result.GetProductsResult;
import com.book.core.product.application.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

@WebMvcTest(ProductController.class)
@Import({ProductCommandConverter.class, ProductResultConverter.class, SwaggerConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocWebMvcConfiguration.class,
    MultipleOpenApiSupportConfiguration.class})
@ActiveProfiles("test")
class ProductControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProductService productService;

    @Test
    void 상품_상세를_명세_필드로_응답한다() throws Exception {
        when(productService.getProductDetail(any())).thenReturn(new GetProductDetailResult(20L, "상품명", "thumbnail.jpg", "작가", "출판사",
            LocalDate.of(2026, 1, 1), new BigDecimal("20000.00"), new BigDecimal("18000.00"), 3L, new BigDecimal("4.5"), 10,
            List.of(new GetProductCouponResult(1L, "ACTIVE", "할인 쿠폰", "PERCENT", new BigDecimal("10.00"), new BigDecimal("10000.00"),
                new BigDecimal("5000.00"), 1, 0, "2026-12-31T23:59:59"))));

        mvc.perform(get("/api/v1/products/{productId}", 20)).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.productId").value(20)).andExpect(jsonPath("$.data.itemId").doesNotExist())
            .andExpect(jsonPath("$.data.itemName").value("상품명")).andExpect(jsonPath("$.data.thumbnailUrl").value("thumbnail.jpg"))
            .andExpect(jsonPath("$.data.author").value("작가")).andExpect(jsonPath("$.data.publisher").value("출판사"))
            .andExpect(jsonPath("$.data.publishedAt").value("2026-01-01")).andExpect(jsonPath("$.data.salePrice").value(20000.00))
            .andExpect(jsonPath("$.data.discountedPrice").value(18000.00)).andExpect(jsonPath("$.data.reviewCount").value(3))
            .andExpect(jsonPath("$.data.reviewRate").value(4.5)).andExpect(jsonPath("$.data.stockQuantity").value(10))
            .andExpect(jsonPath("$.data.coupons[0].id").value(1)).andExpect(jsonPath("$.data.coupons[0].name").value("할인 쿠폰"));

        verify(productService).getProductDetail(any());
    }

    @Test
    void 존재하지_않는_상품은_E404를_응답한다() throws Exception {
        when(productService.getProductDetail(any())).thenThrow(new CoreException(ErrorCode.PRODUCT_NOT_FOUND));

        mvc.perform(get("/api/v1/products/20")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("E404"))
            .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
    }

    @Test
    void 양수가_아닌_상품_ID는_E400으로_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(get("/api/v1/products/0")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(productService);
    }

    @Test
    void 숫자가_아닌_상품_ID는_E400으로_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(get("/api/v1/products/not-a-number")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(productService);
    }

    @Test
    void 저장소_오류는_E500으로_응답한다() throws Exception {
        when(productService.getProductDetail(any())).thenThrow(new IllegalStateException("database detail"));

        mvc.perform(get("/api/v1/products/20")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }

    @Test
    void 상품_목록을_명세_필드로_응답한다() throws Exception {
        when(productService.getProducts(any())).thenReturn(GetProductsResult.of(List.of(new GetProductItemResult(101L, "상품명",
            "thumbnail.jpg", "작가", new BigDecimal("20000.00"), new BigDecimal("18000.00"), 0L, 0L, BigDecimal.ZERO)), "101"));

        mvc.perform(get("/api/v1/items").queryParam("categoryId", "7").queryParam("sort", "createdAt").queryParam("cursor", "102")
            .queryParam("limit", "1")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].itemId").value(101)).andExpect(jsonPath("$.data.items[0].itemName").value("상품명"))
            .andExpect(jsonPath("$.data.items[0].thumbnailUrl").value("thumbnail.jpg"))
            .andExpect(jsonPath("$.data.items[0].author").value("작가")).andExpect(jsonPath("$.data.items[0].salePrice").value(20000.00))
            .andExpect(jsonPath("$.data.items[0].discountedPrice").value(18000.00))
            .andExpect(jsonPath("$.data.items[0].orderCount").value(0)).andExpect(jsonPath("$.data.items[0].reviewCount").value(0))
            .andExpect(jsonPath("$.data.items[0].reviewRate").value(0)).andExpect(jsonPath("$.data.nextCursor").value("101"));

        final var commandCaptor = ArgumentCaptor.forClass(GetProductsCommand.class);
        verify(productService).getProducts(commandCaptor.capture());
        assertThat(commandCaptor.getValue().categoryId()).isEqualTo(7L);
        assertThat(commandCaptor.getValue().sort()).isEqualTo(ProductListSort.CREATED_AT);
        assertThat(commandCaptor.getValue().cursor().productId()).isEqualTo(102L);
        assertThat(commandCaptor.getValue().limit()).isEqualTo(1);
    }

    @Test
    void 인기순_정렬과_복합_커서를_서비스에_전달한다() throws Exception {
        final String cursor = Base64.getUrlEncoder().withoutPadding().encodeToString("12:102".getBytes(StandardCharsets.UTF_8));
        when(productService.getProducts(any())).thenReturn(GetProductsResult.of(List.of(), null));

        mvc.perform(get("/api/v1/items").queryParam("sort", "POPULARITY").queryParam("cursor", cursor)).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        final var commandCaptor = ArgumentCaptor.forClass(GetProductsCommand.class);
        verify(productService).getProducts(commandCaptor.capture());
        assertThat(commandCaptor.getValue().sort()).isEqualTo(ProductListSort.POPULARITY);
        assertThat(commandCaptor.getValue().cursor().salesQuantity()).isEqualTo(12L);
        assertThat(commandCaptor.getValue().cursor().productId()).isEqualTo(102L);
    }

    @Test
    void 상품이_없으면_빈_목록을_응답한다() throws Exception {
        when(productService.getProducts(any())).thenReturn(GetProductsResult.of(List.of(), null));

        mvc.perform(get("/api/v1/items")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray()).andExpect(jsonPath("$.data.items").isEmpty());

        final var commandCaptor = ArgumentCaptor.forClass(GetProductsCommand.class);
        verify(productService).getProducts(commandCaptor.capture());
        assertThat(commandCaptor.getValue().sort()).isEqualTo(ProductListSort.CREATED_AT);
        assertThat(commandCaptor.getValue().cursor()).isNull();
    }

    @Test
    void 양수가_아닌_카테고리와_페이지_크기는_E400이다() throws Exception {
        mvc.perform(get("/api/v1/items").queryParam("categoryId", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/items").queryParam("limit", "0")).andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void 숫자가_아닌_커서는_E400이다() throws Exception {
        mvc.perform(get("/api/v1/items").queryParam("cursor", "not-a-number")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(productService);
    }

    @Test
    void 지원하지_않는_정렬과_인기순_커서는_E400이다() throws Exception {
        mvc.perform(get("/api/v1/items").queryParam("sort", "unknown")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(get("/api/v1/items").queryParam("sort", "POPULARITY").queryParam("cursor", "101")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(productService);
    }

    @Test
    void 상품_상세와_목록_명세가_OpenAPI에_노출된다() throws Exception {
        mvc.perform(get("/v3/api-docs/general")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/products/{productId}'].get.summary").value("상품 상세 조회"))
            .andExpect(jsonPath("$.paths['/api/v1/products/{productId}'].get.parameters[0].name").value("productId"))
            .andExpect(jsonPath("$.paths['/api/v1/products/{productId}'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/items'].get.summary").value("상품 목록 조회"))
            .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[0].name").value("categoryId"))
            .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[1].name").value("sort"))
            .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[2].name").value("cursor"))
            .andExpect(jsonPath("$.paths['/api/v1/items'].get.parameters[3].name").value("limit"))
            .andExpect(jsonPath("$.paths['/api/v1/items'].get.responses['200']").exists());
    }

    @Test
    void 목록_저장소_오류는_E500으로_응답한다() throws Exception {
        when(productService.getProducts(any())).thenThrow(new IllegalStateException("database list"));

        mvc.perform(get("/api/v1/items")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("E500"))
            .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }
}
