package com.book.core.address.application.port;

import com.book.core.address.domain.Address;
import java.util.List;
import java.util.Optional;

public interface AddressRepositoryPort {
    int countActiveByUserId(final Long userId);

    boolean existsActiveDuplicateAddress(final Long userId, final Address address);

    Optional<Address> findActiveDefaultByUserId(final Long userId);

    Address save(final Address address);

    List<Address> findActiveByUserId(final Long userId);
}
