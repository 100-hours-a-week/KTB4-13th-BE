package com.book.common.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer.HeaderNames.HeadersToAdd;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@Slf4j
class KafkaConfig {
    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
        final ConcurrentKafkaListenerContainerFactoryConfigurer configurer, final ConsumerFactory<Object, Object> consumerFactory,
        final DefaultErrorHandler kafkaErrorHandler) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    DeadLetterPublishingRecoverer kafkaDeadLetterRecoverer(final KafkaTemplate<Object, Object> kafka) {
        final var recoverer = new DeadLetterPublishingRecoverer(kafka, (record, exception) -> {
            final String deadLetterTopic = record.topic() + ".DLT";
            log.atWarn().addKeyValue("topic", deadLetterTopic).log("[KAFKA] Kafka 소비 재시도를 소진했습니다.");
            return new TopicPartition(deadLetterTopic, record.partition());
        });
        recoverer.excludeHeader(HeadersToAdd.EX_MSG, HeadersToAdd.EX_STACKTRACE);
        recoverer.setFailIfSendResultIsError(true);
        return recoverer;
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(final DeadLetterPublishingRecoverer kafkaDeadLetterRecoverer) {
        final var handler = new DefaultErrorHandler(kafkaDeadLetterRecoverer, new FixedBackOff(1000L, 3L));
        handler.setRetryListeners((record, exception, attempt) -> log.atWarn().addKeyValue("topic", record.topic())
            .addKeyValue("attempt", attempt).log("[KAFKA] Kafka 메시지 소비를 재시도합니다."));
        return handler;
    }
}
