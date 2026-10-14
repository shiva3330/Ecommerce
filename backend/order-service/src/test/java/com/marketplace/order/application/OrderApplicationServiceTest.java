package com.marketplace.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marketplace.order.api.CreateOrderRequest;
import com.marketplace.order.api.OrderLineRequest;
import com.marketplace.order.api.OrderResponse;
import com.marketplace.order.messaging.OrderPlacedEvent;
import com.marketplace.order.persistence.OrderRepository;
import com.marketplace.order.persistence.OrderSnapshot;
import com.marketplace.order.persistence.OutboxRepository;
import com.marketplace.order.persistence.StoredOrder;
import com.marketplace.order.persistence.StoredOrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXISTING_ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OrderRequestHasher orderRequestHasher;

    private OrderApplicationService orderApplicationService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        orderApplicationService = new OrderApplicationService(
                orderRepository,
                outboxRepository,
                orderRequestHasher,
                objectMapper,
                "order.placed.v1"
        );
    }

    @Test
    void writesOrderItemsAndTheCompleteEventToTheOutboxInTheCreateFlow() throws Exception {
        CreateOrderRequest request = request(2);
        when(orderRequestHasher.hash(request)).thenReturn("hash-1");
        when(orderRepository.insertIfAbsent(
                any(), eq("customer-123"), eq("PENDING"), eq("request-1"), eq("hash-1"), any()
        )).thenReturn(true);

        OrderResponse response = orderApplicationService.placeOrder(request, "request-1");

        assertThat(response.status()).isEqualTo("PENDING");
        verify(orderRepository).insertItems(any(), eq(List.of(new StoredOrderItem(PRODUCT_ID, 2))));
        ArgumentCaptor<UUID> eventId = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxRepository).insert(
                eventId.capture(),
                eq(response.orderId()),
                eq("order.placed.v1"),
                eq(response.orderId().toString()),
                payload.capture(),
                any()
        );
        OrderPlacedEvent event = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .readValue(payload.getValue(), OrderPlacedEvent.class);
        assertThat(event.eventId()).isEqualTo(eventId.getValue());
        assertThat(event.orderId()).isEqualTo(response.orderId());
        assertThat(event.customerId()).isEqualTo("customer-123");
        assertThat(event.items()).containsExactly(new OrderPlacedEvent.Item(PRODUCT_ID, 2));
    }

    @Test
    void replaysTheWinnerWithoutWritingASecondOrderOrOutboxEvent() {
        CreateOrderRequest request = request(2);
        when(orderRequestHasher.hash(request)).thenReturn("hash-1");
        when(orderRepository.insertIfAbsent(
                any(), eq("customer-123"), eq("PENDING"), eq("request-1"), eq("hash-1"), any()
        )).thenReturn(false);
        when(orderRepository.findByCustomerAndIdempotencyKey("customer-123", "request-1"))
                .thenReturn(Optional.of(new StoredOrder(
                        EXISTING_ORDER_ID, "customer-123", "PENDING", "hash-1", Instant.parse("2026-01-01T00:00:00Z")
                )));

        OrderResponse response = orderApplicationService.placeOrder(request, "request-1");

        assertThat(response).isEqualTo(new OrderResponse(EXISTING_ORDER_ID, "PENDING"));
        verify(orderRepository, never()).insertItems(any(), any());
        verify(outboxRepository, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsAReusedKeyWithDifferentPayload() {
        CreateOrderRequest request = request(3);
        when(orderRequestHasher.hash(request)).thenReturn("hash-2");
        when(orderRepository.insertIfAbsent(
                any(), eq("customer-123"), eq("PENDING"), eq("request-1"), eq("hash-2"), any()
        )).thenReturn(false);
        when(orderRepository.findByCustomerAndIdempotencyKey("customer-123", "request-1"))
                .thenReturn(Optional.of(new StoredOrder(
                        EXISTING_ORDER_ID, "customer-123", "PENDING", "different-hash", Instant.now()
                )));

        assertThatThrownBy(() -> orderApplicationService.placeOrder(request, "request-1"))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(outboxRepository, never()).insert(any(), any(), any(), any(), any(), any());
    }

    @Test
    void mapsAStoredOrderToTheReadContract() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        when(orderRepository.findById(EXISTING_ORDER_ID)).thenReturn(Optional.of(new OrderSnapshot(
                EXISTING_ORDER_ID,
                "PENDING",
                "customer-123",
                List.of(new StoredOrderItem(PRODUCT_ID, 2)),
                createdAt
        )));

        assertThat(orderApplicationService.findOrder(EXISTING_ORDER_ID)).contains(
                new com.marketplace.order.api.OrderDetailsResponse(
                        EXISTING_ORDER_ID,
                        "PENDING",
                        "customer-123",
                        List.of(new com.marketplace.order.api.OrderDetailsResponse.OrderLineResponse(PRODUCT_ID, 2)),
                        createdAt
                )
        );
    }

    private CreateOrderRequest request(int quantity) {
        return new CreateOrderRequest("customer-123", List.of(new OrderLineRequest(PRODUCT_ID, quantity)));
    }
}
