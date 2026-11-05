# Glintora — Inventory & E-commerce Platform

Glintora is a centralized inventory-management and e-commerce system for a
fashion/apparel business. One backend and **one database** serve two faces of
the same business:

- **Public storefront** — customers browse published products, view
  variant-level availability and size charts, add to cart and check out.
- **Admin dashboard** (`/admin`) — staff manage the product catalogue, receive
  stock, adjust inventory, process orders and watch low-stock alerts.

The design is driven by
[`glintora_inventory_ecommerce_database_design.md`](glintora_inventory_ecommerce_database_design.md)
— the same document that translates the original Glintora Excel workbook into
this relational system.

## Core principles (from the design doc)

1. **Stock is tracked by product variant (SKU/size/color)**, not by product.
2. **Every stock change is an inventory movement** — an append-only audit
   trail (`inventory_movements`) with a fast summary (`inventory_balances`).
3. **On-hand ≠ available.** Available-to-sell = on hand − active reservations.
4. **Order items snapshot price and cost** so historical profit stays correct.
5. **Publication status ≠ stock status.** A published product can be
   out of stock; the store disables only the sold-out variant.
6. **Checkout reserves stock atomically** (pessimistic row locks), so two
   customers can never buy the same last unit.
7. **All sales channels create orders** in the same system (website today;
   Facebook/WhatsApp/retail channels are seeded reference data).

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 22 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Security, Validation, Cache, Thymeleaf) |
| Database | PostgreSQL 14+ (Docker Compose file included) |
| Migrations | Flyway (`V1` schema, `V2` seed data) |
| Views | Thymeleaf + Spring Security dialect + Bootstrap 5 |
| Security | Session form login, BCrypt, role-based (`ROLE_CUSTOMER` / `ROLE_ADMIN`) |
| Build | Maven |

Full technical detail: [ARCHITECTURE.md](ARCHITECTURE.md).

## Getting started

### 1. Start PostgreSQL

Either use Docker:

```bash
docker compose up -d
```

or use a local PostgreSQL and create the database:

```bash
createdb glintora        # or: psql -U postgres -c 'CREATE DATABASE glintora;'
```

Connection settings live in
[`src/main/resources/application.yml`](src/main/resources/application.yml)
(default `postgres/postgres` on `localhost:5432/glintora`, override with
`DB_USERNAME` / `DB_PASSWORD`).

### 2. Run the application

```bash
mvn spring-boot:run
```

Flyway applies the schema and seed data automatically on first start, and a
default admin account is created:

| | |
|---|---|
| Storefront | http://localhost:8080 |
| Admin dashboard | http://localhost:8080/admin |
| Admin login | `admin@glintora.com` / `admin123` |

> Change the admin password before deploying anywhere real.

### 3. Try the flows

1. Log in as the admin, open **Products**, edit `GLI-001` and **Publish** it.
2. Visit the storefront, open the product, pick a size and add to cart.
3. Register a customer account and check out — stock is reserved.
4. In **Admin → Orders**, confirm → process → **Dispatch** (this posts the
   `sale_dispatch` movement and deducts on-hand stock) → deliver.
5. Watch **Admin → Inventory** show the movement trail, and **Dashboard**
   show low-stock/reorder suggestions.

## Project layout

```text
src/main/java/com/glintora/backend/
├── GlintoraApplication.java     Entry point
├── config/                      Security, seed data, layout advice
├── model/                       JPA entities (one per table) + enums
├── repository/                  Spring Data JPA repositories
├── service/                     Business logic (catalog, inventory, cart, orders, dashboard)
├── controller/                  StoreController (storefront), AdminController (/admin)
└── dto/                         View models (ProductCard, CartLine, DashboardStats…)
src/main/resources/
├── db/migration/                V1__init_schema.sql, V2__seed_data.sql
├── templates/                   Thymeleaf pages (storefront + admin/)
└── static/                      CSS and placeholder product images
database.xlsx                    Original inventory workbook (import planned)
```

## Implementation status

| Phase (design doc §13) | Status |
|---|---|
| 1. Core product catalogue | ✅ Schema, entities, admin CRUD, variants, images, publication workflow |
| 2. Inventory foundation | ✅ Movements, balances, goods receipts, reservations, warehouses; ❌ purchase orders & shipments UI, Excel import |
| 3. Internal operations & reporting | ✅ Dashboard (revenue, low stock, reorder suggestions), receiving & adjustment screens, order processing |
| 4. E-commerce catalogue | ✅ Listing with filters/search/sort, product page with size chart & variant availability |
| 5. Checkout & fulfillment | ✅ Session cart, atomic reservation checkout, confirm/dispatch/deliver/cancel, COD payments; ❌ online payment gateway, returns |
| 6. Advanced operations | ⬜ Not started (forecasting, multi-warehouse transfers, promotions) |

### Notable decisions

- **Cart is session-based** (guest-friendly, validated + reserved at
  checkout). The `carts`/`wishlists` tables from the design doc are deferred
  until accounts-based carts are needed.
- **The Excel workbook importer is not built yet**; seed data ships as a
  Flyway migration so the app runs out of the box. Import must reconcile the
  product-ID mismatches documented in design doc §11 before migrating real data.
- **Measurements UI** is Phase 2; measurement rows currently arrive via data
  migrations (seeded for the sample products).

## Testing

```bash
mvn test          # unit/integration tests (Testcontainers wiring planned)
```
