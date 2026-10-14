# API Gateway

Spring Cloud Gateway is the public edge for the marketplace. It forwards versioned API paths to internal services without embedding service credentials or hostnames in Java code.

## Run

```bash
mvn -pl api-gateway spring-boot:run
```

The default port is `8080` and can be changed with `SERVER_PORT`. Route destinations are configured with `IDENTITY_SERVICE_URL`, `CATALOG_SERVICE_URL`, `ORDER_SERVICE_URL`, and `INVENTORY_SERVICE_URL`.

Health: `GET /actuator/health`.
