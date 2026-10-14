package com.marketplace.order.messaging;

import com.marketplace.order.persistence.OutboxMessage;

@FunctionalInterface
public interface OutboxMessagePublisher {

    void publish(OutboxMessage message) throws Exception;
}
