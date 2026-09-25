package com.book.core.address.infrastructure.persistence.repository;

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
        return jpaRepository.countByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public boolean existsActiveDuplicateAddress(final Long userId, final Address address) {
        return jpaRepository.existsActiveDuplicateAddress(
                userId, address.id(), address.label(), address.address(), address.detailAddress());
    }

    @Override
    public Optional<Address> findActiveDefaultByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndDefaultAddressAndDeletedAtIsNull(userId, true);
    }

    @Override
    public Optional<Address> findActiveByIdAndUserId(final Long addressId, final Long userId) {
        return jpaRepository.findByIdAndUserIdAndDeletedAtIsNull(addressId, userId);
    }

    @Override
    public Optional<Address> findLatestActiveByUserIdExcludingId(final Long userId, final Long addressId) {
        return jpaRepository.findFirstByUserIdAndDeletedAtIsNullAndIdNotOrderByCreatedAtDescIdDesc(userId, addressId);
    }

    @Override
    public Address save(final Address address) {
        return jpaRepository.save(address);
    }

    @Override
    public List<Address> findActiveByUserId(final Long userId) {
        return jpaRepository.findByUserIdAndDeletedAtIsNullOrderByDefaultAddressDescCreatedAtAscIdAsc(userId);
    }
}
