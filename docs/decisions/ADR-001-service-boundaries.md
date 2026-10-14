# ADR-001: Start with service boundaries, not empty microservices

- **Status:** Accepted
- **Date:** 2026-10-11

## Context

The project is intended to demonstrate skills for a software job. A microservice list without a working flow is difficult to evaluate and easy to overstate. A single deployable monolith would be simpler, but it would not demonstrate the requested gateway, Kafka, Redis, and service ownership concepts.

## Decision

Create independently runnable Spring Boot boundaries for identity, catalog, order, inventory, payment, and notification, but implement the first durable vertical slice only in catalog and order. Keep the remaining services as honest integration boundaries with health/configuration and clear next responsibilities.

## Consequences

### Positive

- The repository demonstrates the requested architecture without pretending every service is complete.
- The catalog and order flows provide real code to test and discuss.
- New persistence and event consumers can be added without changing the frontend route model.

### Trade-offs

- Running the complete stack needs more local processes and infrastructure.
- The outbox closes the order-commit/event-loss window, but at-least-once delivery can still repeat an event after a publish acknowledgement; consumers must deduplicate.
- The frontend catalog/auth development fallbacks are intentionally not production behavior; checkout requires the durable order API.

## Revisit when

If the portfolio needs to ship a smaller hosted demo, deploy catalog/order as a modular monolith first and preserve the same API/event contracts. Do not add service count for its own sake.
