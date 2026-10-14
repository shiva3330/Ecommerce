# Ecom — Multi-vendor Marketplace

Ecom is a industry-grade, end-to-end marketplace. A customer can discover products, add them to a cart, check out, and later track an order. A seller can manage a catalog and see a seller-facing dashboard. The platform is intentionally split into independently deployable Spring Boot services behind an API gateway, with Angular as the storefront UI.

The goal is not to claim that a scaffold is a production ecommerce platform. The goal is to provide a coherent, runnable slice and a roadmap that lets you implement, explain, test, and deploy the next slice in a job interview.

## What is already here

- Angular standalone storefront with catalog, search/filter/sort, cart, demo checkout, sign-in, and seller dashboard surfaces.
- Spring Boot Maven reactor with API gateway plus identity, catalog, order, inventory, payment, and notification service boundaries.
- A real catalog `GET /api/v1/products` boundary backed by PostgreSQL/JPA/Flyway, with 12 deterministic products across 3 seeded sellers.
- A durable order `POST /api/v1/orders` boundary backed by PostgreSQL/Flyway, customer-scoped idempotency, a transactional outbox, and an outbox-to-Kafka publisher.
- Redis and Kafka local infrastructure, health endpoints, Testcontainers-ready backend tests, and focused Angular specs.
- Architecture, API, portfolio demo, and implementation guidance under `docs/`.

## First user flow

1. Start PostgreSQL, Redis, and Kafka with Docker Compose.
2. Run the catalog, order, and gateway services.
3. Run the Angular storefront.
4. Browse the seeded catalog (or use the labeled development fallback), add items to the cart, and submit checkout.
5. Read the persisted order confirmation and explain how the outbox emits `order.placed.v1`; inventory, payment, and notification consume that event asynchronously as they are implemented.

## Repository shape

```text
Ecommerce/
├── backend/                         Spring Boot microservices (Maven reactor)
│   ├── api-gateway/                 Edge routing on :8080
│   ├── identity-service/            Authentication boundary on :8081
│   ├── catalog-service/             Products + PostgreSQL on :8082
│   ├── order-service/               Checkout/order intake on :8083
│   ├── inventory-service/           Stock reservation boundary on :8084
│   ├── payment-service/             Payment provider boundary on :8085
│   └── notification-service/        Kafka consumer boundary on :8086
├── frontend/storefront/             Angular standalone app on :4200
├── infra/postgres/init/             Local database bootstrap scripts
├── docs/                            Architecture, contracts, ADRs, roadmap
├── docker-compose.yml                PostgreSQL + Redis + Kafka + Kafka UI
└── .github/workflows/ci.yml         Build/test checks for portfolio visibility
```

## Prerequisites

- Java 21 and Maven 3.9+
- Node.js 20.11+ and npm 10+
- Docker Desktop with Compose v2
- Chrome/Chromium for the Angular headless browser tests

The current environment used to prepare this project has Java 17 and no Docker/Angular CLI, so the full native checks must be rerun on a machine with the prerequisites above.

## Quick start

### 1. Start local infrastructure

PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose up -d
docker compose ps
```

macOS/Linux:

```bash
test -f .env || cp .env.example .env
docker compose up -d
docker compose ps
```

The first PostgreSQL start creates the service databases. If you change `infra/postgres/init/001-create-databases.sh`, reset the disposable local volume before starting again:

```bash
docker compose down -v
docker compose up -d
```

### 2. Build and test the backend

```bash
cd backend
mvn clean verify
```

Run a service with its dependencies available:

```bash
mvn -pl catalog-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl api-gateway spring-boot:run
```

Maven does not automatically load the root `.env` file. On PowerShell, export the catalog settings in the terminal where you start it:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/catalog_db'
$env:DB_USERNAME = 'marketplace'
$env:DB_PASSWORD = 'change_me'
$env:KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'
mvn -pl catalog-service spring-boot:run
```

### 3. Run the Angular storefront

```bash
cd frontend/storefront
npm install
npm start
```

Open `http://localhost:4200`. The UI uses a clearly labeled development fallback when the catalog/auth APIs are not available. Checkout requires the order API so a confirmation always represents a durable order.

