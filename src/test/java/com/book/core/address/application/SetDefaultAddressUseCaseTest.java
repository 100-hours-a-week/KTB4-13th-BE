package com.book.core.address.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.SetDefaultAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.SetDefaultAddressResult;
import com.book.core.address.application.usecase.SetDefaultAddressUseCase;
import com.book.core.address.domain.Address;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SetDefaultAddressUseCaseTest {
    private final FakeAddressRepository addressRepository = new FakeAddressRepository();
    private final SetDefaultAddressUseCase useCase = new SetDefaultAddressUseCase(addressRepository);

    @Test
    void 활성_주소지를_기본_배송지로_지정하고_기존_기본_배송지를_해제한다() {
        final var currentDefault = Address.of(42L, "기존", "11111", "서울시 중구", null, true);
        addressRepository.target = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, false);
        addressRepository.defaultAddress = currentDefault;

        final SetDefaultAddressResult result = useCase.execute(new SetDefaultAddressCommand(42L, 101L));

        assertThat(result.addressId()).isEqualTo(101L);
        assertThat(addressRepository.target.isDefaultAddress()).isTrue();
        assertThat(currentDefault.isDefaultAddress()).isFalse();
        assertThat(addressRepository.savedAddress).isSameAs(addressRepository.target);
    }

    @Test
    void 이미_기본_배송지인_주소지를_다시_지정해도_성공한다() {
        addressRepository.target = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, true);

        final SetDefaultAddressResult result = useCase.execute(new SetDefaultAddressCommand(42L, 101L));

        assertThat(result.addressId()).isEqualTo(101L);
        assertThat(addressRepository.savedAddress).isNull();
    }

    @Test
    void 수정_대상이_없거나_요청_회원의_소유가_아니면_E403으로_거부한다() {
        assertThatThrownBy(() -> useCase.execute(new SetDefaultAddressCommand(42L, 101L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private static final class FakeAddressRepository implements AddressRepositoryPort {
        private Address target;
        private Address defaultAddress;
        private Address savedAddress;

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
            return Optional.ofNullable(defaultAddress);
        }

        @Override
        public Address save(final Address address) {
            savedAddress = address;
            return address;
        }

        @Override
        public List<Address> findActiveByUserId(final Long userId) {
            return List.of();
        }

        @Override
        public Optional<Address> findActiveByIdAndUserId(final Long addressId, final Long userId) {
            if (target != null
                    && target.id().equals(addressId)
                    && target.userId().equals(userId)) {
                return Optional.of(target);
            }
            return Optional.empty();
        }
    }
}
