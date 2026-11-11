package com.book.core.coupon.infrastructure.messaging;

import com.book.core.coupon.application.port.IssuanceRequestPublisher;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
@RequiredArgsConstructor
class IssuanceRequestProducer implements IssuanceRequestPublisher {
    private final KafkaTemplate<String, String> kafka;

    @Value("${coupon.kafka.topic}")
    private String topic;

    @Override
    public void publishAndWait(final String messageKey, final String payload) {
        kafka.send(topic, messageKey, payload).orTimeout(12, TimeUnit.SECONDS).join();
    }
}
