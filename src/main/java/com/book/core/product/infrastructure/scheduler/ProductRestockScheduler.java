package com.book.core.product.infrastructure.scheduler;

import com.book.core.product.application.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductRestockScheduler {
    private final ProductService productService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        receiveRestocks();
        createRestocks();
    }

    private void receiveRestocks() {
        try {
            productService.getReceivableRestocks().forEach(this::receiveRestock);
        } catch (final Exception exception) {
            log.error("[PRODUCT] 입고할 발주 조회에 실패했습니다.", exception);
        }
    }

    private void receiveRestock(final Long restockId) {
        try {
            productService.receiveRestock(restockId);
        } catch (final Exception exception) {
            log.atError().addKeyValue("restockId", restockId).log("[PRODUCT] 발주 입고에 실패했습니다. restockId={}", restockId, exception);
        }
    }

    private void createRestocks() {
        try {
            productService.getRestockCandidates().forEach(this::createRestock);
        } catch (final Exception exception) {
            log.error("[PRODUCT] 발주 대상 상품 조회에 실패했습니다.", exception);
        }
    }

    private void createRestock(final Long productId) {
        try {
            productService.createRestock(productId);
        } catch (final Exception exception) {
            log.atError().addKeyValue("productId", productId).log("[PRODUCT] 상품 발주 생성에 실패했습니다. productId={}", productId, exception);
        }
    }
}
