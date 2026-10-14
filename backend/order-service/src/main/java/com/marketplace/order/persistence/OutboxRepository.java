package com.marketplace.order.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxRepository {

    private static final String INSERT_EVENT = """
            INSERT INTO outbox_events
                (event_id, aggregate_id, topic, event_key, payload, created_at, available_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String CLAIM_READY = """
            SELECT event_id, aggregate_id, topic, event_key, payload, attempts
            FROM outbox_events
            WHERE published_at IS NULL
              AND available_at <= CURRENT_TIMESTAMP
              AND (claimed_until IS NULL OR claimed_until < CURRENT_TIMESTAMP)
            ORDER BY available_at, created_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
            """;

    private static final String CLAIM_ONE = """
            UPDATE outbox_events
            SET claim_token = ?,
                claimed_until = CURRENT_TIMESTAMP + (CAST(? AS double precision) * INTERVAL '1 millisecond'),
                attempts = attempts + 1
            WHERE event_id = ? AND published_at IS NULL
            """;

    private static final String MARK_PUBLISHED = """
            UPDATE outbox_events
            SET published_at = ?, claim_token = NULL, claimed_until = NULL, last_error = NULL
            WHERE event_id = ? AND published_at IS NULL AND claim_token = ?
            """;

    private static final String MARK_FAILED = """
            UPDATE outbox_events
            SET available_at = ?, claim_token = NULL, claimed_until = NULL, last_error = ?
            WHERE event_id = ? AND published_at IS NULL AND claim_token = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public OutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            UUID eventId,
            UUID aggregateId,
            String topic,
            String eventKey,
            String payload,
            Instant createdAt
    ) {
        jdbcTemplate.update(
                INSERT_EVENT,
                eventId,
                aggregateId,
                topic,
                eventKey,
                payload,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt)
        );
    }

    /**
     * Claims are short-lived leases. The database transaction only holds row
     * locks while assigning the lease; Kafka I/O happens after commit.
     */
    @Transactional
    public List<OutboxMessage> claimReady(int batchSize, UUID claimToken, Duration claimTimeout) {
        List<OutboxMessage> candidates = jdbcTemplate.query(
                CLAIM_READY,
                (rs, rowNum) -> new OutboxMessage(
                        rs.getObject("event_id", UUID.class),
                        rs.getObject("aggregate_id", UUID.class),
                        rs.getString("topic"),
                        rs.getString("event_key"),
                        rs.getString("payload"),
                        rs.getInt("attempts") + 1
                ),
                batchSize
        );
        if (candidates.isEmpty()) {
            return candidates;
        }

        for (OutboxMessage message : candidates) {
            jdbcTemplate.update(CLAIM_ONE, claimToken, claimTimeout.toMillis(), message.eventId());
        }
        return candidates;
    }

    public boolean markPublished(UUID eventId, UUID claimToken, Instant publishedAt) {
        return jdbcTemplate.update(MARK_PUBLISHED, Timestamp.from(publishedAt), eventId, claimToken) == 1;
    }

    public boolean markFailed(
            UUID eventId,
            UUID claimToken,
            Instant availableAt,
            String error
    ) {
        return jdbcTemplate.update(
                MARK_FAILED,
                Timestamp.from(availableAt),
                boundError(error),
                eventId,
                claimToken
        ) == 1;
    }

    private String boundError(String error) {
        if (error == null || error.isBlank()) {
            return "outbox delivery failed";
        }
        return error.length() <= 2000 ? error : error.substring(0, 2000);
    }
}
