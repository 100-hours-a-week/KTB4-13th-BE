package com.book.core.address.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.usecase.RegisterAddressUseCase;
import com.book.core.address.domain.Address;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegisterAddressUseCaseTest {
    private final FakeAddressRepository addressRepository = new FakeAddressRepository();
    private final RegisterAddressUseCase useCase = new RegisterAddressUseCase(addressRepository);

    @Test
    void Command의_userId가_양수가_아니면_E400으로_거부한다() {
        assertThatThrownBy(() -> new RegisterAddressCommand(0L, "집", "12345", "서울시 강남구", null, false))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 요청_회원의_주소지를_등록한다() {
        addressRepository.activeCount = 1;

        useCase.execute(new RegisterAddressCommand(7L, "  회사 ", " 12345 ", " 서울시 강남구 ", " 101호 ", false));

        assertThat(addressRepository.savedAddress).satisfies(address -> {
            assertThat(address.userId()).isEqualTo(7L);
            assertThat(address.label()).isEqualTo("회사");
            assertThat(address.postalCode()).isEqualTo("12345");
            assertThat(address.address()).isEqualTo("서울시 강남구");
            assertThat(address.detailAddress()).isEqualTo("101호");
            assertThat(address.isActive()).isTrue();
            assertThat(address.isDefaultAddress()).isFalse();
        });
    }

    @Test
    void 첫_주소는_isDefaultAddress가_false여도_기본_배송지로_등록한다() {
        addressRepository.activeCount = 0;

        useCase.execute(new RegisterAddressCommand(7L, "집", "12345", "서울시 강남구", null, false));

        assertThat(addressRepository.savedAddress.isDefaultAddress()).isTrue();
    }

    @Test
    void isDefaultAddress가_false면_기존_기본_배송지를_유지한다() {
        final var currentDefault = Address.of(7L, "기존", "11111", "서울시 중구", null, true);
        addressRepository.activeCount = 1;
        addressRepository.defaultAddress = currentDefault;

        useCase.execute(new RegisterAddressCommand(7L, "회사", "12345", "서울시 강남구", null, false));

        assertThat(currentDefault.isDefaultAddress()).isTrue();
        assertThat(addressRepository.savedAddress.isDefaultAddress()).isFalse();
    }

    @Test
    void isDefaultAddress가_true면_기존_기본_배송지를_해제하고_새_주소를_기본으로_등록한다() {
        final var currentDefault = Address.of(7L, "기존", "11111", "서울시 중구", null, true);
        addressRepository.activeCount = 1;
        addressRepository.defaultAddress = currentDefault;

        useCase.execute(new RegisterAddressCommand(7L, "회사", "12345", "서울시 강남구", null, true));

        assertThat(currentDefault.isDefaultAddress()).isFalse();
        assertThat(addressRepository.savedAddress.isDefaultAddress()).isTrue();
    }

    @Test
    void 활성_주소지가_세_개면_등록하지_않는다() {
        final var currentDefault = Address.of(7L, "기존", "11111", "서울시 중구", null, true);
        addressRepository.activeCount = 3;
        addressRepository.defaultAddress = currentDefault;

        assertThatThrownBy(() -> useCase.execute(new RegisterAddressCommand(7L, "회사", "12345", "서울시 강남구", null, true)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.ADDRESS_LIMIT_EXCEEDED));

        assertThat(addressRepository.savedAddress).isNull();
        assertThat(currentDefault.isDefaultAddress()).isTrue();
    }

    @Test
    void 활성_주소지_한도_초과는_주소_생성보다_먼저_실패한다() {
        addressRepository.activeCount = 3;

        assertThatThrownBy(() -> useCase.execute(new RegisterAddressCommand(7L, " ", "12345", "서울시 강남구", null, false)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.ADDRESS_LIMIT_EXCEEDED));
    }

    @Test
    void 동일_주소와_동일_별칭은_중복_등록하지_않는다() {
        final var currentDefault = Address.of(7L, "기존", "11111", "서울시 중구", null, true);
        addressRepository.activeCount = 1;
        addressRepository.defaultAddress = currentDefault;
        addressRepository.duplicate = true;

        assertThatThrownBy(() -> useCase.execute(new RegisterAddressCommand(7L, "기존", "11111", "서울시 중구", null, true)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.DUPLICATE_ADDRESS));

        assertThat(addressRepository.savedAddress).isNull();
        assertThat(currentDefault.isDefaultAddress()).isTrue();
    }

    @Test
    void 별칭이_다르면_같은_주소도_등록한다() {
        addressRepository.activeCount = 1;
        addressRepository.duplicate = false;

        useCase.execute(new RegisterAddressCommand(7L, "회사", "11111", "서울시 중구", null, false));

        assertThat(addressRepository.savedAddress.label()).isEqualTo("회사");
    }

    @Test
    void 저장에_실패하면_기존_기본_배송지를_해제하지_않는다() {
        final var currentDefault = Address.of(7L, "기존", "11111", "서울시 중구", null, true);
        addressRepository.activeCount = 1;
        addressRepository.defaultAddress = currentDefault;
        addressRepository.failOnSave = true;

        assertThatThrownBy(() -> useCase.execute(new RegisterAddressCommand(7L, "회사", "12345", "서울시 강남구", null, true)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(currentDefault.isDefaultAddress()).isTrue();
    }

    private static final class FakeAddressRepository implements AddressRepositoryPort {
        private int activeCount;
        private boolean duplicate;
        private boolean failOnSave;
        private Address defaultAddress;
        private Address savedAddress;

        @Override
        public int countActiveByUserId(final Long userId) {
            return activeCount;
        }

        @Override
        public boolean existsActiveDuplicateAddress(final Long userId, final Address address) {
            return duplicate;
        }

        @Override
        public Optional<Address> findActiveDefaultByUserId(final Long userId) {
            return Optional.ofNullable(defaultAddress);
        }

        @Override
        public Address save(final Address address) {
            if (failOnSave) {
                throw new IllegalStateException("save failed");
            }
            savedAddress = address;
            return address;
        }

        @Override
        public List<Address> findActiveByUserId(final Long userId) {
            return List.of();
        }
    }
}
