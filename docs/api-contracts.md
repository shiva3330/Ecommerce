# API and event contracts

The gateway is the public entry point. Public paths use `/api/v1`; internal service URLs are not exposed to the browser. All timestamps are ISO-8601 UTC values. All money values are decimal strings or integer minor units in a real payment integration; never use binary floating point for persisted money.

## Common response rules

| Case | Status | Shape |
| --- | ---: | --- |
| Successful read | 200 | Resource or page object |
| Successful create accepted for async work | 202 | Resource id + current status |
| Validation error | 400 | `code`, `message`, `fieldErrors[]` |
| Missing/unauthorized | 401/403/404 | Problem details without secrets |
| Idempotency replay with the same payload | 202 | The original resource id and status |
| Idempotency key reused with a different payload | 409 | Stable conflict code |
| Unexpected failure | 500 | Stable error code + correlation id |

Example problem response:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Request contains invalid fields",
  "fieldErrors": [
    { "field": "items", "message": "must contain at least one item" }
  ],
  "correlationId": "8caa4c7d-..."
}
```

## Catalog API

### List products

`GET /api/v1/products?query=linen&category=home&sort=price-asc&page=0&size=24`

Current scaffold response is a list. Evolve it to a page object when pagination is implemented:

```json
[
  {
    "id": "11111111-1111-1111-1111-111111111111",
    "sellerId": "seller-123",
    "name": "Linen throw",
    "slug": "linen-throw",
    "description": "Breathable woven cotton-linen throw",
    "category": "home",
    "price": 49.99,
    "currency": "USD",
    "availableQuantity": 12,
    "active": true
  }
]
```

### Seller product commands (next increment)

```text
POST   /api/v1/seller/products
PATCH  /api/v1/seller/products/{productId}
POST   /api/v1/seller/products/{productId}/publish
DELETE /api/v1/seller/products/{productId}
```

Every seller command checks the authenticated seller id and emits a catalog event after commit.

## Identity API (next increment)

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
```

Login request/response:

```json
// request
{ "email": "seller@example.com", "password": "not-a-real-secret" }

// response
{
  "accessToken": "<short-lived-token>",
  "refreshToken": "<rotating-token>",
  "expiresIn": 900,
  "user": { "id": "user-123", "roles": ["SELLER"] }
}
```

Do not put tokens, passwords, or payment data in logs, fixtures, screenshots, or committed environment files.

## Order API

### Create an order

`POST /api/v1/orders`

Headers:

```text
Authorization: Bearer <access-token>
Idempotency-Key: checkout-8f5c...
Content-Type: application/json
```

Request:

```json
{
  "customerId": "customer-123",
  "items": [
    { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 2 }
  ],
  "shippingAddress": {
    "line1": "1 Market Street",
    "city": "Bengaluru",
    "postalCode": "560001",
    "country": "IN"
  },
  "paymentMethod": "DEMO_CARD"
}
```

Create response:

```json
{ "orderId": "22222222-2222-2222-2222-222222222222", "status": "PENDING" }
```

Read/command endpoints:

```text
GET    /api/v1/orders/{orderId}  # implemented; returns the persisted order
GET    /api/v1/orders
POST   /api/v1/orders/{orderId}/cancel
```

The service must derive the customer from the access token in production. Keeping `customerId` in the initial request makes the scaffold easy to test; remove that trust boundary before production.

## Event envelope (next reliability increment)

All Kafka events should use a shared envelope so consumers can trace and deduplicate messages:

```json
{
  "eventId": "33333333-3333-3333-3333-333333333333",
  "eventType": "OrderPlaced.v1",
  "occurredAt": "2026-10-11T10:00:00Z",
  "aggregateType": "ORDER",
  "aggregateId": "22222222-2222-2222-2222-222222222222",
  "correlationId": "8caa4c7d-...",
  "payload": {}
}
```

The current order outbox keeps the durable `eventId` alongside the existing
order payload. Adding this full envelope, correlation IDs, and versioned
consumer contracts is the next reliability increment; consumers must already
deduplicate by the current `eventId`.

### `order.placed.v1`

```json
{
  "orderId": "22222222-2222-2222-2222-222222222222",
  "customerId": "customer-123",
  "items": [
    { "productId": "11111111-1111-1111-1111-111111111111", "quantity": 2 }
  ]
}
```

Consumers:

- inventory: reserve each product quantity;
- payment: create/authorize payment after successful reservation;
- notification: send an order-received message;
- analytics: record conversion metrics later.

### Idempotency rules

- Store `eventId` (or a deterministic consumer key) before acknowledging a message.
- A repeated event must return the previously computed outcome, not reserve/charge/send twice.
- The order API must return the same order and status for a repeated `Idempotency-Key` with the same customer and payload. A different payload under the same customer/key returns `409` with `IDEMPOTENCY_KEY_REUSE`.
- Use a dead-letter topic after bounded retries and make replay an explicit operator action.
