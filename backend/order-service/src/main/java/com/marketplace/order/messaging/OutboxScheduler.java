package com.marketplace.order.messaging;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.outbox.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxScheduler {

    private final OutboxDispatcher dispatcher;

    public OutboxScheduler(OutboxDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.fixed-delay:1000ms}")
    public void publishReadyEvents() {
        dispatcher.dispatchOnce();
    }
}
