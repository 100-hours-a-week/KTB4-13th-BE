package com.book.core.address.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.domain.Address;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryAdapter implements AddressRepositoryPort {
    private final AddressJpaRepository jpaRepository;

    @Override
    public int countActiveByUserId(final Long userId) {
        return jpaRepository.countByUserIdAndStatus(userId, EntityStatus.ACTIVE);
    }

    @Override
    public boolean existsActiveDuplicateAddress(final Long userId, final Address address) {
        return jpaRepository.existsActiveDuplicateAddress(
                userId, address.label(), address.address(), address.detailAddress(), EntityStatus.ACTIVE);
    }

    @Override
    public Optional<Address> findActiveDefaultByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndDefaultAddressAndStatus(userId, true, EntityStatus.ACTIVE);
    }

    @Override
    public Address save(final Address address) {
        return jpaRepository.save(address);
    }
}
