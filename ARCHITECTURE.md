# Glintora — Technical Architecture

This document describes how the Glintora backend is structured and why. It
complements the product/data design in
[`glintora_inventory_ecommerce_database_design.md`](glintora_inventory_ecommerce_database_design.md).

## 1. Big picture

Glintora is a **single Spring Boot monolith** serving two UIs from one
database — the pattern recommended by the design doc (§2):

```text
Browser (storefront)  ─┐                       ┌─ PostgreSQL
Browser (admin)       ─┼─► Spring Boot app ────┤   one schema,
                       │   Thymeleaf MVC       │   Flyway migrations
                       │   Service layer       └─ (catalog, inventory,
                       │   Spring Security           orders, customers)
```

- The **storefront** (`/`, `/shop/**`, `/cart`, `/checkout`) is public.
- The **admin dashboard** (`/admin/**`) requires `ROLE_ADMIN`.
- Both read and write the **same** product, variant, inventory and order
  tables — there is no data synchronization between two systems.

Because both UIs are server-rendered Thymeleaf pages in the same app, there
is no separate REST API layer yet; the service layer is where a future API
would attach (see §7).

## 2. Technology choices

| Concern | Choice | Why |
|---|---|---|
| Runtime | Java 22 + Spring Boot 3.3 | Request/response web app, strong transaction support |
| Persistence | Spring Data JPA (Hibernate) | Rich domain model; repositories for all 29 tables |
| Database | PostgreSQL | Relational integrity, `NUMERIC` money types, row locking for checkout |
| Migrations | Flyway | Versioned, repeatable schema (`V1`) + seed (`V2`) |
| Views | Thymeleaf + Bootstrap 5 | Server-rendered pages; admin & storefront share fragments |
| Security | Spring Security form login + BCrypt | Session-based auth; roles map to the `customers.role` column |
| Build | Maven | Standard Spring Boot tooling |

## 3. Layered structure

```text
controller/   StoreController, AdminController        — HTTP mapping, flash messages
    ↓
service/      CatalogService, CatalogAdminService,
              InventoryService, CartService,
              OrderService, DashboardService,
              CustomerService                          — ALL business rules live here
    ↓
repository/   Spring Data JPA interfaces               — queries, pessimistic locks
    ↓
model/        JPA entities (1:1 with tables) + enums
    ↓
PostgreSQL    Flyway-managed schema
```

DTOs (`dto/`) decouple templates from entities where a computed view is
needed (`ProductCard`, `VariantView`, `CartLine`, `DashboardStats`,
`LowStockLine`).

### Why the service layer matters here

The design doc's key rules are enforced in services, not templates:

- `InventoryService.postMovement()` is the **only** sanctioned way to change
  stock: it writes the movement row **and** updates the balance inside one
  transaction, and refuses to drive on-hand below zero.
- `OrderService.placeOrder()` re-validates stock under a `SELECT … FOR UPDATE`
  lock (`InventoryBalanceRepository.findLocked`) before creating
  reservations — the concurrency-safe checkout required by doc §8.
- `OrderService.dispatch()` converts active reservations into physical
  `sale_dispatch` movements; `cancel()` releases them.

## 4. Inventory model (the heart of the system)

```text
                 purchase / receipt
                        │ (+)
                        ▼
inventory_movements ◄────────────────► inventory_balances (summary)
   (audit log, signed qty)                 quantity_on_hand
                        │ (-)
                 sale / write-off / dispatch

available_to_sell = quantity_on_hand − Σ active stock_reservations
```

- **Two-account design:** `inventory_movements` is append-only history;
  `inventory_balances` (PK `(variant_id, warehouse_id)`) is the cached
  current state, always written in the same transaction as its movement.
- **Reservation vs. dispatch:** checkout only *reserves*
  (`stock_reservations.status = active`), which reduces available-to-sell
  without touching on-hand. Physical stock leaves at *dispatch*.
- **Receiving:** `goods_receipts` + `goods_receipt_items` record what
  physically arrived; only `received − rejected` units generate `receipt`
  movements.
- **Reorder logic** is per variant (`inventory_reorder_rules`):
  `suggested = max(0, target − (available + outstanding ordered PO qty))`.

## 5. Order lifecycle

```text
pending ──confirm──► confirmed ──process──► processing ──dispatch──► shipped ──deliver──► delivered
   │                                                                 (stock-out posted)      │
   └──────────────────── cancel ──────────────────────────────► cancelled                  │
                          (reservations released)                    (COD payment → paid) ◄─┘
```

- Transitions are validated in `OrderService`; every transition appends to
  `order_status_history`.
- `order_items.unit_price` / `unit_cost` are **snapshots** taken at checkout,
  so revenue and margin reports never change when catalogue prices change.
- Payments are recorded per order (`COD` supported; `bKash`/gateway is a
  future integration point).

## 6. Security model

- Accounts are rows in `customers`; `role` carries `ROLE_CUSTOMER` or
  `ROLE_ADMIN` and is loaded by a `UserDetailsService` bean.
- `SecurityConfig` protects `/admin/**` (ADMIN), `/checkout/**` & `/orders/**`
  (authenticated), and leaves browsing public.
- Passwords are BCrypt-hashed; the seeded admin is created at first startup by
  `DataInitializer` if no admin exists.
- CSRF protection stays on for all form posts (Thymeleaf injects tokens).

## 7. Configuration & runtime

| Item | Where | Notes |
|---|---|---|
| Datasource | `application.yml` | `DB_USERNAME`/`DB_PASSWORD` env overrides |
| JPA | `ddl-auto: validate` | Schema is owned by Flyway, never by Hibernate |
| Flyway | `db/migration/` | `V1__init_schema.sql` (30 tables), `V2__seed_data.sql` |
| Server | port `8080` | |

`docker-compose.yml` provides a local PostgreSQL 16 with a named volume and
healthcheck.

## 8. Data model summary

29 tables, all mapped 1:1 by JPA entities:

| Module | Tables |
|---|---|
| Catalogue | `brands`, `categories`, `products`, `product_variants`, `sizes`, `colors`, `product_images`, `product_attributes`, `product_measurements` |
| Purchasing | `suppliers`, `purchase_orders`, `purchase_order_items`, `shipments`, `goods_receipts`, `goods_receipt_items` |
| Inventory | `warehouses`, `inventory_movements`, `inventory_balances`, `stock_reservations`, `inventory_reorder_rules` |
| Sales | `customers`, `customer_addresses`, `sales_channels`, `orders`, `order_items`, `payments`, `order_status_history` |
| Delivery & feedback | `delivery_shipments`, `product_reviews` |

Money is `NUMERIC(12,2)` / `BigDecimal` everywhere; status columns are
`VARCHAR` with CHECK constraints mirrored by lowercase Java enums
(`ProductStatus`, `OrderStatus`, `MovementType`, `ReservationStatus`,
`PaymentStatus`).

## 9. Known trade-offs & roadmap

| Decision now | Rationale | Later |
|---|---|---|
| Session cart | Guests can shop without accounts; stock is re-validated at checkout | DB-backed `carts`/`wishlist_items` tables when account carts are needed |
| Thymeleaf-only UI | Fastest path to a working system; no SPA build | REST/OpenAPI layer (springdoc already on the classpath) reusing the services |
| Single-warehouse checkout | `warehouses` + balances are already multi-warehouse; allocation picks the default warehouse | Cross-warehouse allocation & `transfer_in/out` movements |
| COD payments only | No gateway dependency while flows stabilize | bKash / SSLCommerz integration behind the `Payment` entity |
| Excel import deferred | Workbook has product-ID inconsistencies to reconcile first (doc §11) | Apache POI importer (dependency already included) with validation report |
