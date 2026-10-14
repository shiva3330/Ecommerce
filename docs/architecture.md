# Ecom architecture

## Product scope

Ecom is a marketplace rather than a single-store checkout. The platform has three actors:

- **Customer** — discovers products, manages a cart, checks out, and tracks order status.
- **Seller** — owns a storefront, creates products, manages price/stock, and sees seller orders.
- **Platform operator** — moderates sellers/products, manages refunds, and monitors the system.

The first vertical slice is intentionally small: catalog read, cart/checkout UI, order intake, and an `OrderPlaced` event. Additional services can be implemented without changing the customer-facing contract.

## Context and container view

```text
                                  +----------------------+
                                  | Angular storefront   |
                                  | :4200                |
                                  +----------+-----------+
                                             |
                                             | HTTPS / JSON
                                             v
                                  +----------------------+
                                  | API gateway :8080   |
                                  | routing, CORS, auth |
                                  +----+------+----------+
                                       |      |
                         synchronous    |      | asynchronous
                                       v      v
       +----------------+  +----------------+  +------------------+
       | identity       |  | catalog        |  | order            |
       | :8081          |  | :8082          |  | :8083            |
       +-------+--------+  +-------+--------+  +--------+---------+
               |                   |                    |
               v                   v                    | publishes
       +---------------+   +---------------+             v
       | identity_db   |   | catalog_db    |       +-------------+
       | PostgreSQL    |   | PostgreSQL    |       | Kafka topics |
       +---------------+   +---------------+       +------+------+ 
                                                              |
                           +----------------------------------+----------------+
                           |                  |                 |               |
                           v                  v                 v               v
                    +-------------+   +-------------+   +-------------+  +------------+
                    | inventory   |   | payment     |   | notification|  | analytics  |
                    | :8084       |   | :8085       |   | :8086       |  | later      |
                    +------+------+   +------+------+   +------+------+  +------------+
                           |                  |                 |
                           v                  v                 v
                         Redis            provider          email/SMS
```

## Service ownership

| Service | Owns | Synchronous API | Events consumed/published |
| --- | --- | --- | --- |
| API gateway | Edge policy and routing only | `/api/v1/**` | None |
| Identity | Users, roles, refresh tokens | Register/login/profile | `UserRegistered.v1` later |
| Catalog | Sellers, categories, products, prices | Product and seller catalog APIs | `ProductPublished.v1` later |
| Order | Checkout intent, order state, order lines | Create/read/cancel order | Publishes `OrderPlaced.v1`, consumes fulfillment/payment status |
| Inventory | Stock and reservations | Seller stock APIs | Consumes `OrderPlaced.v1`; publishes `InventoryReserved.v1` or `InventoryRejected.v1` |
| Payment | Payment intent/provider reference | Payment status/refund APIs | Consumes order/inventory events; publishes `PaymentAuthorized.v1` or `PaymentFailed.v1` |
| Notification | Email/SMS templates and delivery attempts | Internal/admin status | Consumes order/payment/fulfillment events |

Each service owns its tables and migrations. Other services must use an API or event rather than reading another service's database.

## Storage choices

### PostgreSQL

Use PostgreSQL for transactional aggregates and reporting-friendly data:

- one logical database per service in local Compose (`catalog_db`, `order_db`, etc.);
- Flyway migrations checked into each service;
- UUID identifiers generated at the service boundary;
- `created_at`, `updated_at`, and optimistic version columns for mutable aggregates;
- `DECIMAL(19,4)` for money, never floating point;
- no cross-service foreign keys.

The current catalog migration is the reference implementation. Order persistence should be the next backend increment.

### Redis

Use Redis only for data that can be rebuilt or expires:

- `cart:{customerId}` with a TTL;
- `inventory:available:{productId}` for read-through stock views;
- short-lived login/rate-limit/session data;
- idempotency keys such as `idempotency:{service}:{key}`.

Do not make Redis the system of record for orders, payments, or seller payouts.

### Kafka

Events are versioned facts, not remote method calls. Recommended topic names:

```text
order.placed.v1
inventory.reserved.v1
inventory.rejected.v1
payment.authorized.v1
payment.failed.v1
order.status-changed.v1
notification.requested.v1
```

Use the order id as the Kafka key to preserve ordering for one order. Consumers must be idempotent because at-least-once delivery is the default. The order service now uses a transactional outbox; consumer idempotency remains a later hardening step.

## Checkout choreography

1. Gateway authenticates the customer and forwards `POST /api/v1/orders`.
2. Order service validates the request, creates a `PENDING` order, and writes an outbox/event record in the same database transaction.
3. An outbox publisher emits `OrderPlaced.v1`.
4. Inventory reserves stock. On failure it emits `InventoryRejected.v1`.
5. Payment authorizes the payment only after inventory is reserved. It emits `PaymentAuthorized.v1` or `PaymentFailed.v1`.
6. Order service advances state using allowed transitions (`PENDING -> CONFIRMED`, `PENDING -> CANCELLED`, etc.).
7. Notification sends a confirmation or failure message and records delivery attempts.

The order request commits its order, items, and durable event payload together. A bounded outbox scheduler claims rows with short leases and marks them published only after Kafka acknowledges the send. A crash after Kafka acknowledgement and before the database update can still produce a duplicate, so consumers must deduplicate by event id.

## Security baseline

- Identity service issues short-lived access tokens and rotating refresh tokens.
- Gateway validates JWT signature and expiry; services still enforce resource-level authorization.
- Roles: `CUSTOMER`, `SELLER`, `ADMIN`.
- Seller queries filter by `seller_id` from the authenticated principal, never by a user-supplied seller id alone.
- Passwords use a slow hash such as BCrypt/Argon2; raw passwords never enter logs or events.
- CORS is explicit per environment. Secrets come from environment variables or a secret manager.
- Add request correlation id and redact authorization, payment, and personal data from logs.

## Reliability and observability

The scaffold exposes Actuator health/info endpoints. Add these before deployment:

- structured JSON logs with `trace_id`, `span_id`, `correlation_id`, `order_id`;
- Micrometer metrics for request latency, Kafka lag, checkout outcomes, and cache hit ratio;
- OpenTelemetry traces across gateway, order, Kafka, and downstream consumers;
- readiness checks for PostgreSQL, Kafka, and Redis, separate from liveness;
- retry with backoff only for transient failures, plus a dead-letter topic;
- idempotency on order creation and every event consumer;
- dashboards and alerts for error rate, latency, consumer lag, and payment failures.

## Deployment direction

For a portfolio deployment, use one Docker image per service and a managed PostgreSQL/Redis/Kafka provider. Kubernetes manifests or Helm charts should include resource requests/limits, readiness probes, secrets, and rolling updates. Do not add Kubernetes before the local vertical slice and tests are reliable; a small, explainable system is stronger in an interview than a collection of empty manifests.
