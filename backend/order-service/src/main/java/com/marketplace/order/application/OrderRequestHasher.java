package com.marketplace.order.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketplace.order.api.CreateOrderRequest;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class OrderRequestHasher {

    private final ObjectMapper objectMapper;

    public OrderRequestHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Hashes a small canonical JSON tree rather than raw request bytes. JSON
     * property ordering and insignificant whitespace therefore do not change
     * the idempotency identity, while item order remains part of the payload.
     */
    public String hash(CreateOrderRequest request) {
        ObjectNode canonical = objectMapper.createObjectNode();
        canonical.put("customerId", request.customerId());
        ArrayNode items = canonical.putArray("items");
        request.items().forEach(item -> {
            ObjectNode line = items.addObject();
            line.put("productId", item.productId().toString());
            line.put("quantity", item.quantity());
        });

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(canonical));
            return HexFormatHolder.format(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash the order request", e);
        }
    }

    private static final class HexFormatHolder {
        private static String format(byte[] bytes) {
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(value & 0x0f, 16));
            }
            return result.toString();
        }
    }
}
