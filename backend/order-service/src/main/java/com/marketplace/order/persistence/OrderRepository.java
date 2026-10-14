package com.marketplace.order.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepository {

    private static final String INSERT_ORDER = """
            INSERT INTO orders (id, customer_id, status, idempotency_key, request_hash, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (customer_id, idempotency_key) DO NOTHING
            """;

    private static final String FIND_BY_KEY = """
            SELECT id, customer_id, status, request_hash, created_at
            FROM orders
            WHERE customer_id = ? AND idempotency_key = ?
            """;

    private static final String FIND_BY_ID = """
            SELECT o.id, o.status, o.customer_id, o.created_at,
                   i.product_id, i.quantity
            FROM orders o
            LEFT JOIN order_items i ON i.order_id = o.id
            WHERE o.id = ?
            ORDER BY i.line_number
            """;

    private static final String INSERT_ITEM = """
            INSERT INTO order_items (order_id, line_number, product_id, quantity)
            VALUES (?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * The unique constraint is the concurrency boundary. PostgreSQL waits for
     * an in-flight conflicting insert before returning from this statement, so
     * the subsequent load observes the winner or can insert after a rollback.
     */
    public boolean insertIfAbsent(
            UUID orderId,
            String customerId,
            String status,
            String idempotencyKey,
            String requestHash,
            Instant createdAt
    ) {
        return jdbcTemplate.update(
                INSERT_ORDER,
                orderId,
                customerId,
                status,
                idempotencyKey,
                requestHash,
                Timestamp.from(createdAt)
        ) == 1;
    }

    public Optional<StoredOrder> findByCustomerAndIdempotencyKey(String customerId, String idempotencyKey) {
        return jdbcTemplate.query(
                FIND_BY_KEY,
                (rs, rowNum) -> new StoredOrder(
                        rs.getObject("id", UUID.class),
                        rs.getString("customer_id"),
                        rs.getString("status"),
                        rs.getString("request_hash"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                customerId,
                idempotencyKey
        ).stream().findFirst();
    }

    public void insertItems(UUID orderId, List<StoredOrderItem> items) {
        List<Object[]> arguments = new ArrayList<>(items.size());
        for (int lineNumber = 0; lineNumber < items.size(); lineNumber++) {
            StoredOrderItem item = items.get(lineNumber);
            arguments.add(new Object[]{orderId, lineNumber, item.productId(), item.quantity()});
        }
        jdbcTemplate.batchUpdate(INSERT_ITEM, arguments);
    }

    public Optional<OrderSnapshot> findById(UUID orderId) {
        List<OrderSnapshotRow> rows = jdbcTemplate.query(
                FIND_BY_ID,
                (rs, rowNum) -> new OrderSnapshotRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("status"),
                        rs.getString("customer_id"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getObject("product_id", UUID.class),
                        rs.getObject("quantity", Integer.class)
                ),
                orderId
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }

        OrderSnapshotRow first = rows.get(0);
        List<StoredOrderItem> items = new ArrayList<>();
        for (OrderSnapshotRow row : rows) {
            if (row.productId() != null) {
                items.add(new StoredOrderItem(row.productId(), row.quantity()));
            }
        }
        return Optional.of(new OrderSnapshot(
                first.orderId(),
                first.status(),
                first.customerId(),
                List.copyOf(items),
                first.createdAt()
        ));
    }

    private record OrderSnapshotRow(
            UUID orderId,
            String status,
            String customerId,
            Instant createdAt,
            UUID productId,
            Integer quantity
    ) {
    }
}
