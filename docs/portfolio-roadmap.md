# Job-ready portfolio roadmap

## Target outcome

Be able to demo one complete customer journey in 5–7 minutes, then explain the engineering decisions behind it. A recruiter should see a working UI; an engineer should see ownership boundaries, tests, failure handling, and a realistic path from scaffold to production.

## Milestones

### Milestone 1 — Foundation (done)

- Angular standalone shell and lazy routes.
- Catalog page with search/filter/sort and development data.
- Cart and demo checkout experience.
- Spring services with clear ownership and Actuator endpoints.
- Catalog persistence with JPA + Flyway.
- Validated order endpoint and Kafka publisher port/adapter.
- Local Docker Compose, CI workflow, architecture and contracts docs.

### Milestone 2 — Real vertical slice (recommended next)

1. Seed `catalog_db` with 12 products and 3 sellers. **Implemented.**
2. Add order persistence with `orders` and `order_items` migrations. **Implemented.**
3. Replace the direct order Kafka publish with a transactional outbox. **Implemented.**
4. Add gateway CORS and route the Angular API base URL to `http://localhost:8080/api/v1`. **Implemented.**
5. Add a real `GET /api/v1/orders/{id}` and wire checkout confirmation to it. **Implemented.**
6. Add an end-to-end test that starts PostgreSQL/Kafka with Testcontainers or a Compose profile. **Remaining: requires Docker.**

**Definition of done:** a checkout creates a durable order and a retry with the same idempotency key does not create a duplicate.

### Milestone 3 — Marketplace behavior

1. Implement identity registration/login with JWT and BCrypt/Argon2.
2. Add seller product create/update/publish endpoints with `SELLER` authorization.
3. Add inventory reservations using PostgreSQL as the source of truth and Redis as a cache/fast read model.
4. Add order state transitions driven by inventory and payment events.
5. Add seller dashboard APIs for product and order summaries.

**Definition of done:** a seller can publish a product, a customer can buy it, and a second purchase cannot oversell the available stock.

### Milestone 4 — Reliability and production signals

1. Add a shared event envelope, schema versioning, and dead-letter topics.
2. Add idempotent consumers and retry/backoff policies.
3. Add correlation IDs, structured logs, Micrometer metrics, and OpenTelemetry traces.
4. Add rate limiting and request size limits at the gateway.
5. Add API documentation (OpenAPI) and contract tests.
6. Add Dockerfiles and a small deployment target (Render/Fly.io/Railway or a cloud VM; managed data services are fine).

**Definition of done:** a failed payment or consumer retry is visible, safe, and recoverable without manually editing a database.

## Suggested 4-week execution plan

| Week | Focus | Interview evidence |
| --- | --- | --- |
| 1 | Order persistence, outbox, seed data | SQL migrations, unit/integration tests, idempotency |
| 2 | Identity/JWT and seller product APIs | RBAC, validation, secure password handling |
| 3 | Inventory reservation and event-driven status | Kafka choreography, Redis cache, race-condition test |
| 4 | Observability, Dockerfiles, deployment, polish | CI badge, health checks, architecture diagram, demo |

## Portfolio demo script

1. Start with the problem: multiple sellers share a customer marketplace but services must own their own data.
2. Open the storefront and search/filter products.
3. Add two products and change quantity in the cart.
4. Complete demo checkout and show the pending order response.
5. Show the order controller, transactional outbox, and Kafka delivery adapter.
6. Open Kafka UI and explain how inventory, payment, and notification consume `order.placed.v1`.
7. Show the Flyway catalog migration, validation tests, health endpoint, and CI workflow.
8. State the honest gap: the order outbox is durable and at-least-once, while consumer idempotency, JWT/RBAC, and inventory reservations are the next production steps.

## Questions you should be ready to answer

- Why not one database for all microservices?
- What happens when inventory succeeds but payment fails?
- How do you prevent duplicate orders when the browser retries?
- Why Kafka instead of a synchronous HTTP chain?
- What is Redis allowed to store, and what is the source of truth?
- How would you secure seller APIs against changing another seller's products?
- How do you test a Kafka consumer and a PostgreSQL migration?
- What would you monitor in production?

## Resume bullet template

> Built a multi-vendor marketplace with Angular and Spring Boot microservices, using PostgreSQL/Flyway for service-owned transactional data, Kafka for asynchronous order events, Redis for cache/idempotency concerns, and Docker Compose/CI for repeatable local development; implemented validated catalog/order boundaries and an accessible storefront with focused tests.

Only claim the pieces you have actually implemented and can explain. Update the bullet as each milestone becomes real.
