package com.marketplace.order.messaging;

import com.marketplace.order.persistence.OutboxMessage;
import com.marketplace.order.persistence.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxRepository outboxRepository;
    private final OutboxMessagePublisher publisher;
    private final int batchSize;
    private final Duration claimTimeout;
    private final Duration retryBaseDelay;
    private final Duration retryMaxDelay;

    public OutboxDispatcher(
            OutboxRepository outboxRepository,
            OutboxMessagePublisher publisher,
            @Value("${app.outbox.scheduler.batch-size:50}") int batchSize,
            @Value("${app.outbox.scheduler.claim-timeout:30s}") Duration claimTimeout,
            @Value("${app.outbox.scheduler.retry-base-delay:1s}") Duration retryBaseDelay,
            @Value("${app.outbox.scheduler.retry-max-delay:5m}") Duration retryMaxDelay
    ) {
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException("outbox batch size must be between 1 and 1000");
        }
        if (claimTimeout.isZero() || claimTimeout.isNegative() || claimTimeout.toMillis() < 1) {
            throw new IllegalArgumentException("outbox claim timeout must be positive");
        }
        if (retryBaseDelay.isZero() || retryBaseDelay.isNegative()) {
            throw new IllegalArgumentException("outbox retry base delay must be positive");
        }
        if (retryMaxDelay.isZero() || retryMaxDelay.isNegative() || retryMaxDelay.compareTo(retryBaseDelay) < 0) {
            throw new IllegalArgumentException("outbox retry max delay must be >= the base delay");
        }
        this.outboxRepository = outboxRepository;
        this.publisher = publisher;
        this.batchSize = batchSize;
        this.claimTimeout = claimTimeout;
        this.retryBaseDelay = retryBaseDelay;
        this.retryMaxDelay = retryMaxDelay;
    }

    public int dispatchOnce() {
        int processed = 0;
        // Claim just before sending. Leasing a whole batch up front would let
        // later rows expire while earlier Kafka sends block during an outage.
        while (processed < batchSize && !Thread.currentThread().isInterrupted()) {
            UUID claimToken = UUID.randomUUID();
            List<OutboxMessage> messages = outboxRepository.claimReady(1, claimToken, claimTimeout);
            if (messages.isEmpty()) {
                break;
            }
            OutboxMessage message = messages.get(0);
            processed++;
            try {
                publisher.publish(message);
                if (!outboxRepository.markPublished(message.eventId(), claimToken, Instant.now())) {
                    log.warn("Outbox event {} was acknowledged but its lease was no longer owned", message.eventId());
                }
            } catch (Exception exception) {
                Duration delay = retryDelay(message.attempts());
                Instant availableAt = Instant.now().plus(delay);
                outboxRepository.markFailed(
                        message.eventId(),
                        claimToken,
                        availableAt,
                        describe(exception)
                );
                log.warn(
                        "Outbox event {} delivery failed; retrying after {}",
                        message.eventId(),
                        delay,
                        exception
                );
            }
        }
        return processed;
    }

    private Duration retryDelay(int attempts) {
        int exponent = Math.min(Math.max(attempts - 1, 0), 30);
        long multiplier = 1L << exponent;
        Duration candidate;
        try {
            candidate = retryBaseDelay.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            candidate = retryMaxDelay;
        }
        return candidate.compareTo(retryMaxDelay) > 0 ? retryMaxDelay : candidate;
    }

    private String describe(Exception exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
