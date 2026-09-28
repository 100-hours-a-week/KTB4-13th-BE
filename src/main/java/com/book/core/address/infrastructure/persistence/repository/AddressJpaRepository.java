package com.book.core.address.infrastructure.persistence.repository;

import com.book.core.address.domain.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AddressJpaRepository extends JpaRepository<Address, Long> {
    int countByUserIdAndDeletedAtIsNull(final Long userId);

    Optional<Address> findByUserIdAndDefaultAddressAndDeletedAtIsNull(final Long userId, final boolean defaultAddress);

    Optional<Address> findByIdAndUserIdAndDeletedAtIsNull(final Long id, final Long userId);

    Optional<Address> findFirstByUserIdAndDeletedAtIsNullAndIdNotOrderByCreatedAtDescIdDesc(
            final Long userId, final Long id);

    List<Address> findByUserIdAndDeletedAtIsNullOrderByDefaultAddressDescCreatedAtAscIdAsc(final Long userId);

    @Query("""
            select case when count(address) > 0 then true else false end
            from Address address
            where address.userId = :userId
              and address.deletedAt is null
              and (:addressId is null or address.id <> :addressId)
              and address.label = :label
              and address.address = :address
              and ((:detailAddress is null and address.detailAddress is null)
                   or address.detailAddress = :detailAddress)
            """)
    boolean existsActiveDuplicateAddress(
            @Param("userId") final Long userId,
            @Param("addressId") final Long addressId,
            @Param("label") final String label,
            @Param("address") final String address,
            @Param("detailAddress") final String detailAddress);
}
