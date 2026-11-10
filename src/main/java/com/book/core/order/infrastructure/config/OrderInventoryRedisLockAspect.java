package com.book.core.order.infrastructure.config;

import com.book.core.order.application.command.CreateOrderCommand;
import com.book.core.order.application.command.CreateOrderItemCommand;
import com.book.core.product.application.port.ProductInventoryLockPort;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "app.product.inventory.redis-lock", name = "enabled", havingValue = "true")
public class OrderInventoryRedisLockAspect {
    private final ProductInventoryLockPort inventoryLockPort;

    @Around("execution(* com.book.core.order.application.service.OrderService.createOrder(..))")
    public Object lockInventoryBeforeOrderTransaction(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("재고 Redis 락은 주문 DB 트랜잭션 시작 전에 획득해야 합니다.");
        }
        final CreateOrderCommand command = (CreateOrderCommand) joinPoint.getArgs()[0];
        final List<Long> productIds =
            command.items().stream().map((final CreateOrderItemCommand item) -> item.productId()).distinct().sorted().toList();
        final Map<Long, String> lockTokensByProductId = inventoryLockPort.acquireAll(productIds);
        try {
            return joinPoint.proceed();
        } finally {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new IllegalStateException("재고 Redis 락은 주문 DB 트랜잭션 완료 후 해제해야 합니다.");
            }
            inventoryLockPort.releaseAll(lockTokensByProductId);
        }
    }
}
