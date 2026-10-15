package com.book.core.cart.application.service;

import com.book.core.cart.application.command.AddCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemCommand;
import com.book.core.cart.application.command.DeleteCartItemsCommand;
import com.book.core.cart.application.command.GetCartCommand;
import com.book.core.cart.application.command.ModifyCartItemCommand;
import com.book.core.cart.application.result.GetCartResult;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemsUseCase;
import com.book.core.cart.application.usecase.FindUserIdsWithoutCartUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import com.book.core.cart.application.usecase.RecoverCartUseCase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final AddCartItemUseCase addUseCase;
    private final DeleteCartItemUseCase deleteCartItemUseCase;
    private final DeleteCartItemsUseCase deleteCartItemsUseCase;
    private final GetCartUseCase getCartUseCase;
    private final ModifyCartItemUseCase modifyCartItemUseCase;
    private final FindUserIdsWithoutCartUseCase findUserIdsWithoutCartUseCase;
    private final RecoverCartUseCase recoverCartUseCase;

    public void addCartItem(final AddCartItemCommand command) {
        addUseCase.execute(command);
    }

    public GetCartResult getCart(final GetCartCommand command) {
        return getCartUseCase.execute(command.userId());
    }

    public void modifyCartItem(final ModifyCartItemCommand command) {
        modifyCartItemUseCase.execute(command);
    }

    public void deleteCartItem(final DeleteCartItemCommand command) {
        deleteCartItemUseCase.execute(command);
    }

    public void deleteCartItems(final DeleteCartItemsCommand command) {
        deleteCartItemsUseCase.execute(command);
    }

    public void recoverMissingCarts(final LocalDateTime registeredBefore, final int pageSize) {
        int target = 0;
        int recovered = 0;
        int skipped = 0;
        final Map<String, Integer> failureCauses = new TreeMap<>();
        Long lastUserId = 0L;
        while (true) {
            final List<Long> userIds = findUserIdsWithoutCartUseCase.execute(registeredBefore, lastUserId, pageSize);
            for (final Long userId : userIds) {
                try {
                    if (recoverCartUseCase.execute(userId)) {
                        recovered++;
                    } else {
                        skipped++;
                    }
                } catch (final RuntimeException exception) {
                    failureCauses.merge(exception.getClass().getSimpleName(), 1, Integer::sum);
                    log.warn("장바구니 누락 복구에 실패했습니다. userId={}, cause={}", userId, exception.getClass().getSimpleName(), exception);
                }
            }
            target += userIds.size();
            if (userIds.size() < pageSize) {
                break;
            }
            lastUserId = userIds.getLast();
        }
        log.info("장바구니 누락 복구를 마쳤습니다. target={}, recovered={}, skipped={}, failed={}, failureCauses={}", target, recovered, skipped,
            target - recovered - skipped, failureCauses);
    }
}
