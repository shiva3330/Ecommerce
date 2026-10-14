# Notification Service

Notification consumes marketplace events and is the integration boundary for email, SMS, and in-app delivery. Kafka and Redis are configured through environment variables so delivery providers and idempotency storage can evolve independently.

## Run

Set `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_NOTIFICATION_GROUP`, and the Redis variables, then run:

```bash
mvn -pl notification-service spring-boot:run
```

Default port: `8086`; health: `GET /actuator/health`.
