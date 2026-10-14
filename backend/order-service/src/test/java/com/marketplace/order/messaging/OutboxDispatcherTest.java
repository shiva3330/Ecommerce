package com.marketplace.order.messaging;

import com.marketplace.order.persistence.OutboxMessage;
import com.marketplace.order.persistence.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxMessagePublisher publisher;

    private OutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new OutboxDispatcher(
                outboxRepository,
                publisher,
                10,
                Duration.ofSeconds(30),
                Duration.ofSeconds(1),
                Duration.ofSeconds(10)
        );
    }

    @Test
    void marksAnEventPublishedOnlyAfterThePublisherReturns() throws Exception {
        UUID eventId = UUID.randomUUID();
        OutboxMessage message = new OutboxMessage(
                eventId,
                UUID.randomUUID(),
                "order.placed.v1",
                "order-key",
                "{\"eventId\":\"" + eventId + "\"}",
                1
        );
        when(outboxRepository.markPublished(any(), any(), any())).thenReturn(true);
        when(outboxRepository.claimReady(eq(1), any(), eq(Duration.ofSeconds(30))))
                .thenReturn(List.of(message), List.of());

        assertThat(dispatcher.dispatchOnce()).isEqualTo(1);

        var deliveryOrder = inOrder(publisher, outboxRepository);
        deliveryOrder.verify(publisher).publish(message);
        deliveryOrder.verify(outboxRepository).markPublished(eq(eventId), any(), any(Instant.class));
        verify(outboxRepository, never()).markFailed(any(), any(), any(), any());
    }

    @Test
    void retainsAnEventAndSchedulesBoundedBackoffAfterFailure() throws Exception {
        UUID eventId = UUID.randomUUID();
        OutboxMessage message = new OutboxMessage(
                eventId,
                UUID.randomUUID(),
                "order.placed.v1",
                "order-key",
                "{}",
                4
        );
        when(outboxRepository.claimReady(eq(1), any(), eq(Duration.ofSeconds(30))))
                .thenReturn(List.of(message), List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("broker unavailable"))
                .when(publisher).publish(message);

        Instant before = Instant.now();
        dispatcher.dispatchOnce();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> availableAt = ArgumentCaptor.forClass(Instant.class);
        verify(outboxRepository).markFailed(
                eq(eventId),
                any(),
                availableAt.capture(),
                eq("IllegalStateException: broker unavailable")
        );
        assertThat(availableAt.getValue()).isBetween(before.plusSeconds(8), after.plusSeconds(8));
        verify(outboxRepository, never()).markPublished(any(), any(), any());
    }
}
