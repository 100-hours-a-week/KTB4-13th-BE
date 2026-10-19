package com.book.core.payment.infrastructure.client.toss;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = TossPaymentFeignClient.class)
class TossPaymentConfiguration {
}
