# Inventory Service

Inventory is the consistency boundary for sellable quantities and reservations. Redis is available for low-latency reservation state and cache integration; durable inventory modeling can be added behind this service boundary.

## Run

Set `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD` as needed, then run:

```bash
mvn -pl inventory-service spring-boot:run
```

Default port: `8084`; health: `GET /actuator/health`.
