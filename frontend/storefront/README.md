# Ecom Storefront

An Angular 19-compatible standalone frontend foundation for a multi-vendor marketplace. The storefront includes a responsive shell, catalog discovery, cart, demo checkout, sign-in, and seller dashboard surfaces with typed models and service boundaries ready for a real API.

## Requirements

- Node.js 20.11+ (or the current Node.js LTS)
- npm 10+
- Chrome or Chromium available for the Karma browser test command

## Run locally

From this directory (`marketplace-platform/frontend/storefront`):

```bash
npm install
npm start
```

Then open [http://localhost:4200](http://localhost:4200). The catalog, cart, checkout, and seller preview are usable without a running backend because the app has clearly labeled development fallbacks.

## Exact checks

```bash
npm run build
npm run test:ci
```

For an interactive test session:

```bash
npm test
```

## Service URLs and API contract

The development environment uses:

| Service | URL |
| --- | --- |
| Storefront dev server | `http://localhost:4200` |
| API base URL | `http://localhost:8080/api/v1` |
| Catalog endpoint | `GET http://localhost:8080/api/v1/products` |
| Login endpoint | `POST http://localhost:8080/api/v1/auth/login` |
| Orders endpoint | `POST http://localhost:8080/api/v1/orders` |

The production environment placeholder is `https://api.ecom.example.com/api/v1`. Change the environment file when wiring a real deployment:

- Development: `src/environments/environment.development.ts`
- Production/default: `src/environments/environment.ts`

`ApiService` owns the base-URL join and typed HTTP calls. `authInterceptor` adds `Authorization: Bearer <token>` when `AuthService` has an access token. The intended API response shapes are represented by the models in `src/app/models`.

## Development fallbacks

- `CatalogService` falls back to `src/app/data/development-catalog.ts` when `GET /products` fails. The catalog displays a **Development fallback active** banner.
- `AuthService` creates a local demo session when `POST /auth/login` fails. Email addresses containing `seller` receive the seller role; the login page is prefilled with `seller@example.com` and `demo123` for portfolio demos.
- `OrderService` creates a local `DEMO-...` confirmation when `POST /orders` fails. Checkout identifies this as a **Development order fallback**.

These fallbacks contain no external images, real payment handling, API keys, or other secrets. The checkout intentionally accepts only a demo payment choice until a payment provider is integrated.

## Portfolio demo script

1. Run `npm install` and `npm start`, then open `http://localhost:4200`.
2. Point out the responsive navigation and accessible focus/skip-link behavior.
3. Search for `linen`, switch categories, and change the sort order. Call out the development fallback banner if the API is not running.
4. Add two products, open **Cart**, change a quantity, remove an item, and show free shipping after the threshold.
5. Continue to **Checkout**, enter delivery details, choose the demo card option, and place the demo order to show the confirmation state.
6. Open **Sign in**. Keep the prefilled `seller@example.com` / `demo123` demo values and sign in to open the seller dashboard.
7. Highlight the typed service boundaries, auth interceptor, standalone lazy routes, responsive CSS, and focused tests.

## Project shape

```text
src/
  app/
    data/              Development catalog fixture
    interceptors/      Auth HTTP interceptor and spec
    models/            Product, cart, auth, and checkout types
    pages/             Catalog, cart, checkout, login, seller dashboard
    services/          API, catalog, cart, auth, and order services
    shared/shell/      Responsive navigation and router shell
  environments/        API base URLs
  main.ts              Standalone bootstrap
  styles.css           Accessible global theme and primitives
```
