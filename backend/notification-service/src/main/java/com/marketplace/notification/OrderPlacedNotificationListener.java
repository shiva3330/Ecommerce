package com.marketplace.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedNotificationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderPlacedNotificationListener.class);

    @KafkaListener(
            topics = "${app.kafka.topics.order-placed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onOrderPlaced(String eventPayload) {
        LOGGER.info("Received OrderPlaced event for notification processing: {}", eventPayload);
    }
}
