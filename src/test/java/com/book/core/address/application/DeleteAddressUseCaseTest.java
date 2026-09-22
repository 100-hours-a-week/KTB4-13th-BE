package com.book.core.address.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.DeleteAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.usecase.DeleteAddressUseCase;
import com.book.core.address.domain.Address;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteAddressUseCaseTest {
    private final FakeAddressRepository addressRepository = new FakeAddressRepository();
    private final DeleteAddressUseCase useCase = new DeleteAddressUseCase(addressRepository);

    @Test
    void 기본_배송지_삭제_시_가장_최근의_활성_주소지를_기본으로_승격한다() {
        final Address target = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, true);
        final Address latestAddress = new Address(103L, 42L, "회사", "06237", "서울시 중구", null, false);
        addressRepository.target = target;
        addressRepository.latestActiveAddress = latestAddress;

        useCase.execute(new DeleteAddressCommand(42L, 101L));

        assertThat(target.isDeleted()).isTrue();
        assertThat(target.isDefaultAddress()).isFalse();
        assertThat(latestAddress.isDefaultAddress()).isTrue();
        assertThat(addressRepository.savedAddress).isSameAs(target);
    }

    @Test
    void 일반_주소지_삭제_시_기존_기본_배송지를_유지한다() {
        final Address target = new Address(102L, 42L, "회사", "06237", "서울시 중구", null, false);
        final Address currentDefault = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, true);
        addressRepository.target = target;
        addressRepository.latestActiveAddress = currentDefault;

        useCase.execute(new DeleteAddressCommand(42L, 102L));

        assertThat(target.isDeleted()).isTrue();
        assertThat(currentDefault.isDefaultAddress()).isTrue();
        assertThat(addressRepository.savedAddress).isSameAs(target);
    }

    @Test
    void 마지막_활성_주소지를_삭제하면_기본_배송지를_비워둔다() {
        final Address target = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, true);
        addressRepository.target = target;

        useCase.execute(new DeleteAddressCommand(42L, 101L));

        assertThat(target.isDeleted()).isTrue();
        assertThat(target.isDefaultAddress()).isFalse();
        assertThat(addressRepository.latestActiveAddress).isNull();
    }

    @Test
    void 존재하지_않는_주소지면_E403으로_거부한다() {
        assertThatThrownBy(() -> useCase.execute(new DeleteAddressCommand(42L, 101L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 다른_회원의_주소지면_E403으로_거부한다() {
        addressRepository.target = new Address(101L, 99L, "집", "06236", "서울시 강남구", null, true);

        assertThatThrownBy(() -> useCase.execute(new DeleteAddressCommand(42L, 101L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 비활성_주소지면_E403으로_거부한다() {
        final Address target = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, true);
        target.delete();
        addressRepository.target = target;

        assertThatThrownBy(() -> useCase.execute(new DeleteAddressCommand(42L, 101L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 이미_삭제된_주소지를_다시_삭제하면_E403으로_거부한다() {
        final Address target = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, true);
        target.delete();
        addressRepository.target = target;

        assertThatThrownBy(() -> useCase.execute(new DeleteAddressCommand(42L, 101L)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private static final class FakeAddressRepository implements AddressRepositoryPort {
        private Address target;
        private Address latestActiveAddress;
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
            return Optional.empty();
        }

        @Override
        public Optional<Address> findActiveByIdAndUserId(final Long addressId, final Long userId) {
            if (target != null
                    && target.isActive()
                    && target.id().equals(addressId)
                    && target.userId().equals(userId)) {
                return Optional.of(target);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Address> findLatestActiveByUserIdExcludingId(final Long userId, final Long addressId) {
            return Optional.ofNullable(latestActiveAddress);
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
    }
}
