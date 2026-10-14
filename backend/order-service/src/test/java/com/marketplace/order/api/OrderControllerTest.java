package com.marketplace.order.api;

import com.marketplace.order.application.OrderApplicationService;
import com.marketplace.order.application.IdempotencyConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderApplicationService orderApplicationService;

    @Test
    void rejectsAnOrderWithoutAnIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void rejectsAnOrderWithAWhitespaceOnlyKey() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "   ")
                        .contentType(APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void rejectsAnOverlyLongIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "x".repeat(256))
                        .contentType(APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    @Test
    void rejectsAnOrderWithANullNestedItemOrQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "request-1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"customerId\":\"customer-123\",\"items\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "request-2")
                        .contentType(APPLICATION_JSON)
                        .content("{\"customerId\":\"customer-123\",\"items\":[{\"productId\":\"11111111-1111-1111-1111-111111111111\",\"quantity\":null}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "request-3")
                        .contentType(APPLICATION_JSON)
                        .content("{\"customerId\":\"customer-123\",\"items\":[{\"productId\":\"not-a-uuid\",\"quantity\":1}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsAValidOrderWithTheHeader() throws Exception {
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        given(orderApplicationService.placeOrder(any(), eq("request-1")))
                .willReturn(new OrderResponse(orderId, "PENDING"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "request-1")
                        .contentType(APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void exposesTheStableConflictCode() throws Exception {
        given(orderApplicationService.placeOrder(any(), eq("request-conflict")))
                .willThrow(new IdempotencyConflictException());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "request-conflict")
                        .contentType(APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSE"));
    }

    @Test
    void returnsTheReadContractAnd404ForAnAbsentOrder() throws Exception {
        UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        given(orderApplicationService.findOrder(orderId)).willReturn(Optional.of(new OrderDetailsResponse(
                orderId,
                "PENDING",
                "customer-123",
                List.of(new OrderDetailsResponse.OrderLineResponse(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"), 2
                )),
                Instant.parse("2026-01-01T00:00:00Z")
        )));
        given(orderApplicationService.findOrder(UUID.fromString("33333333-3333-3333-3333-333333333333")))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value("customer-123"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/v1/orders/{id}", "33333333-3333-3333-3333-333333333333"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    private String validBody() {
        return "{\"customerId\":\"customer-123\",\"items\":[{\"productId\":\"11111111-1111-1111-1111-111111111111\",\"quantity\":2}]}";
    }
}
