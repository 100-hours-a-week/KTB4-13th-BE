package com.book.core.address.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.address.application.command.ListAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.ListAddressItemResult;
import com.book.core.address.application.result.ListAddressResult;
import com.book.core.address.application.usecase.ListAddressUseCase;
import com.book.core.address.domain.Address;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListAddressUseCaseTest {
    private final FakeAddressRepository addressRepository = new FakeAddressRepository();
    private final ListAddressUseCase useCase = new ListAddressUseCase(addressRepository);

    @Test
    void 요청_회원의_활성_주소지_목록을_조회_결과로_변환한다() {
        addressRepository.addresses = List.of(
                new Address(101L, 42L, "집", "06236", "서울특별시 강남구 테헤란로 1", "101호", true),
                new Address(102L, 42L, "회사", "06237", "서울특별시 강남구 테헤란로 2", null, false));

        final ListAddressResult result = useCase.execute(new ListAddressCommand(42L));

        assertThat(addressRepository.requestedUserId).isEqualTo(42L);
        assertThat(result.addresses())
                .extracting(
                        ListAddressItemResult::addressId,
                        ListAddressItemResult::addressLabel,
                        ListAddressItemResult::addressPostalCode,
                        ListAddressItemResult::address,
                        ListAddressItemResult::detailAddress,
                        ListAddressItemResult::defaultAddress)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(101L, "집", "06236", "서울특별시 강남구 테헤란로 1", "101호", true),
                        org.assertj.core.groups.Tuple.tuple(102L, "회사", "06237", "서울특별시 강남구 테헤란로 2", null, false));
    }

    @Test
    void 주소지가_없으면_빈_목록을_반환한다() {
        final ListAddressResult result = useCase.execute(new ListAddressCommand(42L));

        assertThat(result.addresses()).isEmpty();
    }

    private static final class FakeAddressRepository implements AddressRepositoryPort {
        private List<Address> addresses = List.of();
        private Long requestedUserId;

        @Override
        public int countActiveByUserId(final Long userId) {
            return 0;
        }

        @Override
        public boolean existsActiveDuplicateAddress(final Long userId, final Address address) {
            return false;
        }

        @Override
        public Optional<Address> findActiveDefaultByUserId(final Long userId) {
            return Optional.empty();
        }

        @Override
        public Address save(final Address address) {
            return address;
        }

        @Override
        public List<Address> findActiveByUserId(final Long userId) {
            requestedUserId = userId;
            return addresses;
        }
    }
}
