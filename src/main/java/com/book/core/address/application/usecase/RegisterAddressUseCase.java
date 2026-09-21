package com.book.core.address.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.port.AddressRepositoryPort;
import com.book.core.address.domain.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RegisterAddressUseCase {
    private static final int MAX_ACTIVE_ADDRESS_COUNT = 3;

    private final AddressRepositoryPort addressRepository;

    @Transactional
    public void execute(final RegisterAddressCommand command) {
        // 활성 주소지가 최대 개수에 도달하면 예외 발생
        final int activeAddressCount = addressRepository.countActiveByUserId(command.userId());
        if (activeAddressCount >= MAX_ACTIVE_ADDRESS_COUNT) {
            throw new CoreException(ErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        final Address address = Address.register(
                command.userId(), command.label(), command.postalCode(), command.address(), command.detailAddress());
        // 완전히 동일한 주소지(주소, 상세주소, 별칭)가 이미 존재하면 예외 발생
        if (addressRepository.existsActiveDuplicateAddress(command.userId(), address)) {
            throw new CoreException(ErrorCode.DUPLICATE_ADDRESS);
        }

        // 첫 번째 주소지인지 확인
        final boolean firstAddress = activeAddressCount == 0;
        Address currentDefaultAddress = null;
        // 기본 배송지로 요청한 경우 기존 기본 배송지를 조회
        if (command.isDefaultAddress() && !firstAddress) {
            currentDefaultAddress = addressRepository
                    .findActiveDefaultByUserId(command.userId())
                    .orElse(null);
        }

        // 첫 번째 주소지이거나 기본 배송지로 요청한 경우 기본 배송지로 설정
        if (firstAddress || command.isDefaultAddress()) {
            address.setDefaultAddress();
        }
        // 주소지 저장
        addressRepository.save(address);
        // 신규 주소지 저장에 성공한 경우 기존 기본 배송지를 해제
        if (currentDefaultAddress != null) {
            currentDefaultAddress.unsetDefaultAddress();
        }
    }
}
