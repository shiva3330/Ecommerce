package com.marketplace.order.application;

import com.marketplace.order.api.CreateOrderRequest;
import com.marketplace.order.api.OrderDetailsResponse;
import com.marketplace.order.api.OrderResponse;
import com.marketplace.order.messaging.OrderPlacedEvent;
import com.marketplace.order.persistence.OrderRepository;
import com.marketplace.order.persistence.OrderSnapshot;
import com.marketplace.order.persistence.OutboxRepository;
import com.marketplace.order.persistence.StoredOrder;
import com.marketplace.order.persistence.StoredOrderItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderApplicationService {

    private static final String PENDING = "PENDING";

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final OrderRequestHasher orderRequestHasher;
    private final ObjectMapper objectMapper;
    private final String orderPlacedTopic;

    public OrderApplicationService(
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            OrderRequestHasher orderRequestHasher,
            ObjectMapper objectMapper,
            @Value("${app.kafka.topics.order-placed}") String orderPlacedTopic
    ) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.orderRequestHasher = orderRequestHasher;
        this.objectMapper = objectMapper;
        this.orderPlacedTopic = orderPlacedTopic;
    }

    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request, String idempotencyKey) {
        String requestHash = orderRequestHasher.hash(request);
        UUID orderId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        boolean inserted = orderRepository.insertIfAbsent(
                orderId,
                request.customerId(),
                PENDING,
                idempotencyKey,
                requestHash,
                createdAt
        );

        if (!inserted) {
            StoredOrder storedOrder = orderRepository.findByCustomerAndIdempotencyKey(request.customerId(), idempotencyKey)
                    .orElseThrow(() ->
                    new IllegalStateException("Idempotency insert conflict did not leave an order row"));
            if (!storedOrder.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException();
            }
            return new OrderResponse(storedOrder.orderId(), storedOrder.status());
        }

        List<StoredOrderItem> storedItems = request.items().stream()
                .map(item -> new StoredOrderItem(item.productId(), item.quantity()))
                .toList();
        orderRepository.insertItems(orderId, storedItems);

        UUID eventId = UUID.randomUUID();
        OrderPlacedEvent event = new OrderPlacedEvent(
                eventId,
                orderId,
                request.customerId(),
                storedItems.stream()
                        .map(item -> new OrderPlacedEvent.Item(item.productId(), item.quantity()))
                        .toList(),
                createdAt
        );
        outboxRepository.insert(
                eventId,
                orderId,
                orderPlacedTopic,
                orderId.toString(),
                serialize(event),
                createdAt
        );

        return new OrderResponse(orderId, PENDING);
    }

    public Optional<OrderDetailsResponse> findOrder(UUID orderId) {
        return orderRepository.findById(orderId).map(this::toResponse);
    }

    private OrderDetailsResponse toResponse(OrderSnapshot snapshot) {
        return new OrderDetailsResponse(
                snapshot.orderId(),
                snapshot.status(),
                snapshot.customerId(),
                snapshot.items().stream()
                        .map(item -> new OrderDetailsResponse.OrderLineResponse(item.productId(), item.quantity()))
                        .toList(),
                snapshot.createdAt()
        );
    }

    private String serialize(OrderPlacedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize the order event", e);
        }
    }
}
