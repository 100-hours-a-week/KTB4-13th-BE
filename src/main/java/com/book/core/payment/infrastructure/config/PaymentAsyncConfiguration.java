package com.book.core.payment.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** 참고 결제 후처리의 비동기 실행을 활성화합니다. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
class PaymentAsyncConfiguration {
}
