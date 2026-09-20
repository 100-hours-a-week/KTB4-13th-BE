package com.book.core.address.infrastructure.persistence.repository;

import com.book.common.domain.EntityStatus;
import com.book.core.address.domain.Address;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AddressJpaRepository extends JpaRepository<Address, Long> {
    int countByUserIdAndStatus(final Long userId, final EntityStatus status);

    Optional<Address> findByUserIdAndDefaultAddressAndStatus(
            final Long userId, final boolean defaultAddress, final EntityStatus status);

    @Query("""
            select case when count(address) > 0 then true else false end
            from Address address
            where address.userId = :userId
              and address.status = :status
              and address.label = :label
              and address.address = :address
              and ((:detailAddress is null and address.detailAddress is null)
                   or address.detailAddress = :detailAddress)
            """)
    boolean existsActiveDuplicateAddress(
            @Param("userId") final Long userId,
            @Param("label") final String label,
            @Param("address") final String address,
            @Param("detailAddress") final String detailAddress,
            @Param("status") final EntityStatus status);
}
