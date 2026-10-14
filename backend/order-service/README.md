# Order Service

Order accepts a validated order command, stores the order and its items in PostgreSQL, and records an `OrderPlacedEvent` in a transactional outbox. A bounded scheduler claims outbox rows with short leases and publishes their durable JSON to Kafka topic `order.placed.v1` after waiting for the broker acknowledgement. Delivery is at-least-once: a crash after Kafka acknowledgement and before the database update can produce a duplicate, so consumers should deduplicate by `eventId`.

## Run

Set `ORDER_DB_URL` (default `jdbc:postgresql://localhost:5432/order_db`),
`ORDER_DB_USERNAME`, `ORDER_DB_PASSWORD`, and `KAFKA_BOOTSTRAP_SERVERS`, then
run:

```bash
mvn -pl order-service spring-boot:run
```

Default port: `8083`.

## API

`POST /api/v1/orders` requires a printable, non-blank `Idempotency-Key` header of at most 255 characters, a non-blank `customerId`, and at least one line item with a UUID product id and positive quantity. A valid request returns `202 Accepted` with `{orderId,status}`. The key is scoped to `customerId`; an identical retry returns the same response, while a different payload returns `409` with code `IDEMPOTENCY_KEY_REUSE`.

```bash
curl -X POST http://localhost:8083/api/v1/orders \
  -H 'Idempotency-Key: checkout-123' \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"customer-123","items":[{"productId":"11111111-1111-1111-1111-111111111111","quantity":2}]}'
```

`GET /api/v1/orders/{id}` returns `{orderId,status,customerId,items,createdAt}` and returns `404` when the order does not exist.

For tests or a worker-managed deployment, set `OUTBOX_SCHEDULER_ENABLED=false` and invoke the dispatcher from the host application. Batch size, claim/send timeouts, polling delay, and exponential retry bounds are configurable with the `OUTBOX_SCHEDULER_*` environment variables.
