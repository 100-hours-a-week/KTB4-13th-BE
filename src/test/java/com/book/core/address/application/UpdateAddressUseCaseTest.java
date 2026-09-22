package com.book.core.address.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.UpdateAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.application.result.UpdateAddressResult;
import com.book.core.address.application.usecase.UpdateAddressUseCase;
import com.book.core.address.domain.Address;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class UpdateAddressUseCaseTest {
    private final FakeAddressRepository addressRepository = new FakeAddressRepository();
    private final UpdateAddressUseCase useCase = new UpdateAddressUseCase(addressRepository);

    @Test
    void 전체_주소지_정보를_정규화해_수정하고_기본_배송지_상태는_유지한다() {
        addressRepository.target = new Address(101L, 42L, "집", "06236", "서울시 강남구", "101호", true);

        final UpdateAddressResult result =
                useCase.execute(new UpdateAddressCommand(42L, 101L, " 회사 ", " 12345 ", " 서울시 중구 ", " 202호 "));

        assertThat(result.addressId()).isEqualTo(101L);
        assertThat(addressRepository.savedAddress).satisfies(address -> {
            assertThat(address.label()).isEqualTo("회사");
            assertThat(address.postalCode()).isEqualTo("12345");
            assertThat(address.address()).isEqualTo("서울시 중구");
            assertThat(address.detailAddress()).isEqualTo("202호");
            assertThat(address.isDefaultAddress()).isTrue();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"  "})
    void detailAddress에_null이나_공백을_전달하면_NULL로_수정한다(final String detailAddress) {
        addressRepository.target = new Address(101L, 42L, "집", "06236", "서울시 강남구", "101호", false);

        useCase.execute(new UpdateAddressCommand(42L, 101L, "집", "06236", "서울시 강남구", detailAddress));

        assertThat(addressRepository.savedAddress.detailAddress()).isNull();
    }

    @Test
    void 동일한_활성_주소지가_있으면_E400으로_거부한다() {
        addressRepository.target = new Address(101L, 42L, "집", "06236", "서울시 강남구", null, false);
        addressRepository.duplicate = true;

        assertThatThrownBy(() -> useCase.execute(new UpdateAddressCommand(42L, 101L, "회사", "06236", "서울시 강남구", null)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.DUPLICATE_ADDRESS));

        assertThat(addressRepository.savedAddress).isNull();
    }

    @Test
    void 수정_대상이_없거나_요청_회원의_소유가_아니면_E403으로_거부한다() {
        assertThatThrownBy(() -> useCase.execute(new UpdateAddressCommand(42L, 101L, "회사", "06236", "서울시 강남구", null)))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private static final class FakeAddressRepository implements AddressRepositoryPort {
        private Address target;
        private boolean duplicate;
        private Address savedAddress;

        @Override
        public int countActiveByUserId(final Long userId) {
            return 0;
        }

        @Override
        public boolean existsActiveDuplicateAddress(final Long userId, final Address address) {
            return duplicate;
        }

        @Override
        public Optional<Address> findActiveDefaultByUserId(final Long userId) {
            return Optional.empty();
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

        @Override
        public Optional<Address> findLatestActiveByUserIdExcludingId(final Long userId, final Long addressId) {
            return Optional.empty();
        }
    }
}
