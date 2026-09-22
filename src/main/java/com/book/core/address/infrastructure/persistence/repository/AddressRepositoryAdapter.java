package com.book.core.address.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.domain.Address;
import java.util.List;
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
                userId, address.id(), address.label(), address.address(), address.detailAddress(), EntityStatus.ACTIVE);
    }

    @Override
    public Optional<Address> findActiveDefaultByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndDefaultAddressAndStatus(userId, true, EntityStatus.ACTIVE);
    }

    @Override
    public Optional<Address> findActiveByIdAndUserId(final Long addressId, final Long userId) {
        return jpaRepository.findByIdAndUserIdAndStatus(addressId, userId, EntityStatus.ACTIVE);
    }

    @Override
    public Optional<Address> findLatestActiveByUserIdExcludingId(final Long userId, final Long addressId) {
        return jpaRepository.findFirstByUserIdAndStatusAndIdNotOrderByCreatedAtDescIdDesc(
                userId, EntityStatus.ACTIVE, addressId);
    }

    @Override
    public Address save(final Address address) {
        return jpaRepository.save(address);
    }

    @Override
    public List<Address> findActiveByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndStatusOrderByDefaultAddressDescCreatedAtAscIdAsc(
                userId, EntityStatus.ACTIVE);
    }
}
