package com.book.core.merchant.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.merchant.application.port.MerchantRepositoryPort;
import com.book.core.merchant.domain.Merchant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetMerchantsUseCase {
    private final MerchantRepositoryPort merchantRepository;

    @Transactional(readOnly = true)
    public List<Merchant> execute(final Collection<Long> merchantIds) {
        return merchantRepository.findAllById(merchantIds);
    }
}
