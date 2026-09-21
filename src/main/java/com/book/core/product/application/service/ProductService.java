package com.book.core.product.application.service;

import com.book.core.product.application.command.GetProductDetailCommand;
import com.book.core.product.application.result.GetProductDetailResult;
import com.book.core.product.application.usecase.GetProductDetailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final GetProductDetailUseCase getProductDetailUseCase;

    public GetProductDetailResult getProductDetail(final GetProductDetailCommand command) {
        return getProductDetailUseCase.execute(command);
    }
}
