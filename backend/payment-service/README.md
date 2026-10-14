# Payment Service

Payment is the boundary for provider authorization, capture, refund, and webhook handling. Provider URL and credentials are configuration inputs only; no provider SDK or secret is embedded in this foundation.

## Run

Set `PAYMENT_PROVIDER_BASE_URL` and `PAYMENT_PROVIDER_API_KEY` in the runtime environment, then run:

```bash
mvn -pl payment-service spring-boot:run
```

Default port: `8085`; health: `GET /actuator/health`.
