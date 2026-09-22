package com.book.core.product.application.service;

import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.command.GetProductsCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.result.GetProductsResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import com.book.core.product.application.usecase.GetProductsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final GetProductDetailUseCase getProductDetailUseCase;
    private final GetProductsUseCase getProductsUseCase;

    public GetProductDetailResult getProductDetail(final GetProductDetailCommand command) {
        return getProductDetailUseCase.execute(command);
    }

    public GetProductsResult getProducts(final GetProductsCommand command) {
        return getProductsUseCase.execute(command);
    }
}
