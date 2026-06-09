# Shop Storefront

A customer-facing storefront for the **shop API**, generated from `openapi.yaml`.

- **React 18 + Vite + TypeScript** — fast SPA, no server runtime needed
- **TanStack Query** — data fetching, caching, loading/error states
- **React Router** — client-side routing
- **Tailwind CSS v4** — styling
- Typed API client in `src/api/` (hand-written DTOs + a thin `fetch` wrapper)

## Features (storefront scope)

- Browse products, filter by category, client-side search
- Product detail page with reviews + write-a-review form
- Cart (persisted in `localStorage`)
- Checkout — create/identify a customer and place an order
- Order history per customer

## Prerequisites

This project needs **Node.js 18+** (this machine did not have Node installed).

```bash
# Ubuntu: install Node via NodeSource, or use nvm:
#   curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
#   nvm install --lts
node -v   # should print v18+ or v20+
```

## Getting started

```bash
npm install
cp .env.example .env     # then edit VITE_API_BASE_URL if needed
npm run dev              # http://localhost:5173
```

The browser-side API base defaults to a relative `/api`, which the dev server
(and the nginx container) proxy to the backend — so there's no CORS. Point the
**dev proxy** at your backend in `vite.config.ts` (`server.proxy["/api"].target`,
default `http://localhost:8080`). You can still force direct cross-origin calls
by setting an absolute URL in `.env` (`VITE_API_BASE_URL=http://host:port/api`),
but then the backend must allow CORS for `http://localhost:5173`.

## Scripts

| Command           | Description                                              |
| ----------------- | -------------------------------------------------------- |
| `npm run dev`     | Start the Vite dev server                                |
| `npm run build`   | Type-check and build for production (`dist/`)            |
| `npm run preview` | Preview the production build                             |
| `npm run gen`     | Regenerate types from `openapi.yaml` via openapi-typescript |

## Container (Podman)

Multi-stage build (`node:20-alpine` → `nginx:alpine`). nginx listens on **5173**
and **reverse-proxies `/api`** to the backend, so the browser only makes
same-origin requests (no CORS). The proxy target is set at runtime via
`API_UPSTREAM` (no rebuild needed).

```bash
podman build -t shop-storefront:latest .

# Host networking lets the container reach a backend bound to the host's
# 127.0.0.1:8080. Open http://localhost:5173
podman run -d --name shop-storefront --network=host \
  -e API_UPSTREAM=http://127.0.0.1:8080 shop-storefront:latest

# or with compose:
podman compose up --build      # serves on http://localhost:5173
```

> **Why host networking?** If the backend is bound only to `127.0.0.1`, a normal
> bridged container can't reach it (even via `host.containers.internal`).
> `--network=host` shares the host's loopback, so `proxy_pass 127.0.0.1:8080`
> works. If your backend instead listens on `0.0.0.0`, you can drop
> `--network=host`, publish with `-p 5173:5173`, and set
> `-e API_UPSTREAM=http://host.containers.internal:8080`.

The dev server proxies `/api` too — see `vite.config.ts` (`server.proxy`),
targeting `http://localhost:8080`.

## API client

`src/api/types.ts` holds hand-written DTOs mirroring the spec's component
schemas, and `src/api/client.ts` is a small typed `fetch` wrapper exposing the
storefront subset of endpoints. If the backend spec changes, run
`npm run gen` to emit `src/api/schema.d.ts` (full `paths`/`components` types),
and adapt the client to use them if you prefer fully generated types.

## Notes / assumptions

- **No auth:** the spec defines no security schemes, so "signing in" just means
  creating or loading a `CustomerDto`, stored in `localStorage`.
- **Cart is client-side:** the API models order items against orders (not a
  live cart), so the cart lives in `localStorage` and is turned into an
  `OrderDto` (with embedded `orderItems`) at checkout.
- **CORS:** the API must allow requests from the dev origin
  (`http://localhost:5173`).
```
