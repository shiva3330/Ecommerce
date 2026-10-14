# Marketplace Platform Backend

This directory contains the Maven multi-module backend foundation for a multi-vendor marketplace. Each module is a separately runnable Spring Boot 3.4 service targeting Java 21.

## Services

| Module | Default port | Responsibility |
| --- | ---: | --- |
| `api-gateway` | 8080 | Edge routing to internal services |
| `identity-service` | 8081 | Identity and authentication boundary |
| `catalog-service` | 8082 | Product catalog and PostgreSQL persistence |
| `order-service` | 8083 | Order intake and `OrderPlaced` event publication |
| `inventory-service` | 8084 | Inventory boundary and Redis integration point |
| `payment-service` | 8085 | Payment-provider integration boundary |
| `notification-service` | 8086 | Kafka-driven notification boundary |

Every service exposes Actuator health and info endpoints at `/actuator/health` and `/actuator/info`.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop with Compose v2 for local PostgreSQL, Kafka, Redis, and the optional Testcontainers integration test

No dependencies are installed by this project setup. Copy `.env.example` to a local environment file or export the variables in your shell; do not commit real credentials.

## Build and test

From this directory:

```bash
mvn clean verify
```

The catalog controller test and order validation/event tests run without external services. `CatalogPostgresIntegrationTest` is Testcontainers-ready and is skipped automatically when Docker is unavailable.

Run one module and its required parent build:

```bash
mvn -pl catalog-service -am test
mvn -pl order-service -am test
```

Run a service locally after its dependencies are available:

```bash
mvn -pl catalog-service spring-boot:run
mvn -pl order-service spring-boot:run
```

Maven does not load `.env` files by itself. For the catalog service on PowerShell, set its local database variables in the same terminal first:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/catalog_db'
$env:DB_USERNAME = 'marketplace'
$env:DB_PASSWORD = 'change_me'
mvn -pl catalog-service spring-boot:run
```

Override the port with `SERVER_PORT`; all service configuration is read from environment variables in each module's `src/main/resources/application.yml`.

## Local dependencies

The canonical local stack lives at the project root. Start it before running services:

```bash
cd ..
docker compose up -d
cd backend
```

It provides PostgreSQL 16, Redis 7, Kafka 3.9, and Kafka UI at `http://localhost:8090`. The first PostgreSQL start creates the service databases, including `catalog_db`. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, and the Redis variables through the environment or a private `.env` file. The application does not embed production credentials or provider API keys.

## REST boundaries

### Catalog

`GET http://localhost:8082/api/v1/products` (or the gateway's `http://localhost:8080/api/v1/products`) returns active products ordered by name. The `catalog-service` owns the `catalog_db` product and seller tables and applies the storefront schema and deterministic seed through Flyway.

### Orders

`POST http://localhost:8083/api/v1/orders` (or the gateway's `http://localhost:8080/api/v1/orders`) accepts a validated payload:

```json
{
  "customerId": "customer-123",
  "items": [
    { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 2 }
  ]
}
```

The endpoint requires a printable `Idempotency-Key` header and returns `202 Accepted` with a pending order id. The order, item rows, and `OrderPlacedEvent` JSON are committed together in `order_db`; a bounded outbox scheduler publishes the durable row to Kafka topic `order.placed.v1` after broker acknowledgement. An identical retry returns the same order, while a different payload under the same customer/key returns `409 IDEMPOTENCY_KEY_REUSE`. Use `GET /api/v1/orders/{id}` to read the persisted order.

## Production direction

The modules intentionally keep business behavior small while establishing boundaries for independent persistence, messaging, validation, health checks, and external provider configuration. JWT/RBAC, inventory reservations, idempotent consumers, observability, and deployment manifests remain future product capabilities.
