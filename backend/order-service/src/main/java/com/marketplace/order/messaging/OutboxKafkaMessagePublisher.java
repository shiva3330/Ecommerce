package com.marketplace.order.messaging;

import com.marketplace.order.persistence.OutboxMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class OutboxKafkaMessagePublisher implements OutboxMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Duration sendTimeout;

    public OutboxKafkaMessagePublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @org.springframework.beans.factory.annotation.Value("${app.outbox.scheduler.send-timeout:10s}") Duration sendTimeout
    ) {
        if (sendTimeout.isZero() || sendTimeout.isNegative() || sendTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("outbox send timeout must be positive");
        }
        this.kafkaTemplate = kafkaTemplate;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public void publish(OutboxMessage message) throws Exception {
        try {
            kafkaTemplate.send(message.topic(), message.eventKey(), message.payload())
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (ExecutionException | TimeoutException exception) {
            throw exception;
        }
    }
}
