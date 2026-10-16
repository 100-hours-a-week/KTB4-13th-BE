package com.book.core.merchant.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.merchant.application.port.MerchantRepositoryPort;
import com.book.core.merchant.domain.MerchantProductMapping;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetMerchantProductMappingsUseCase {
    private final MerchantRepositoryPort merchantRepository;

    @Transactional(readOnly = true)
    public List<MerchantProductMapping> execute(final Collection<Long> productIds) {
        return merchantRepository.findByProductIdIn(productIds);
    }
}
