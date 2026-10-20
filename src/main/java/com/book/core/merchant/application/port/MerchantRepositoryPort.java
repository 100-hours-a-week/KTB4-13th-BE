package com.book.core.merchant.application.port;

import com.book.core.merchant.domain.Merchant;
import com.book.core.merchant.domain.MerchantProductMapping;
import java.util.Collection;
import java.util.List;

public interface MerchantRepositoryPort {
    List<Merchant> findAllById(final Collection<Long> merchantIds);

    List<MerchantProductMapping> findByProductIdIn(final Collection<Long> productIds);
}
