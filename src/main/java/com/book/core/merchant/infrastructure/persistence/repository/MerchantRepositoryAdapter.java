package com.book.core.merchant.infrastructure.persistence.repository;

import com.book.core.merchant.application.port.MerchantRepositoryPort;
import com.book.core.merchant.domain.Merchant;
import com.book.core.merchant.domain.MerchantProductMapping;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MerchantRepositoryAdapter implements MerchantRepositoryPort {
    private final MerchantJpaRepository merchantRepository;
    private final MerchantProductMappingJpaRepository merchantProductMappingRepository;

    @Override
    public List<Merchant> findAllById(final Collection<Long> merchantIds) {
        return merchantRepository.findAllById(merchantIds);
    }

    @Override
    public List<MerchantProductMapping> findByProductIdIn(final Collection<Long> productIds) {
        return merchantProductMappingRepository.findByProductIdIn(productIds);
    }
}
