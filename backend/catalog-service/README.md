# Catalog Service

Catalog owns product discovery and its PostgreSQL schema. Flyway applies migrations from `src/main/resources/db/migration`; Hibernate runs in `validate` mode so schema changes remain explicit and reviewable.

## Run

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`, then run:

```bash
mvn -pl catalog-service spring-boot:run
```

Default port: `8082`.

## API

`GET /api/v1/products` returns active products ordered by name. The response
matches the storefront `Product` shape, including category, rating and review
count, demo availability, a seller summary, tags, and featured state. Example:

```bash
curl http://localhost:8082/api/v1/products
```

Flyway creates three catalog-owned demo sellers and seeds 12 deterministic
products on a fresh database: four products for each seller ID below.

| Seller ID | Seller |
| --- | --- |
| `10000000-0000-4000-8000-000000000001` | Harbor & Loom |
| `10000000-0000-4000-8000-000000000002` | Second Clay |
| `10000000-0000-4000-8000-000000000003` | North Common |

The `inventory` response field is demo availability only. Inventory-service is
the planned source of truth; catalog does not reserve or decrement stock.

The service includes a MockMvc boundary/response-shape test and a Docker-aware
Testcontainers PostgreSQL test for seed integrity and migration reruns.
