# Glintora Inventory & E-commerce Database Design

## Purpose

This document translates the existing Glintora inventory workbook into a relational database design that can support both:

1. An internal inventory and operations dashboard.
2. A customer-facing e-commerce website.

The recommended approach is to use **one centralized database and one backend/API layer** for both systems. The admin dashboard manages products, purchasing, stock, and orders; the e-commerce website displays approved products and accepts customer orders through the same backend.

> **Core design principle:** Inventory must be tracked at the product-variant level (for example, a particular product in size M and color Black), not only at the overall product level.

---

## 1. Understanding the Existing Workbook

The workbook models a fashion/apparel inventory operation. Products are registered, measurements are recorded, quantities are tracked by size, sales are logged, and the remaining stock is used to plan future shipments or reorders.

### 1.1 Workbook sheets and their responsibilities

| Workbook sheet | Current responsibility | Proposed database area |
|---|---|---|
| Product Master | Stores product identity, brand, category, cost, selling price, and other product details | Product catalogue |
| Size Measurements | Stores manufacturer measurements for each size and garment section | Size management |
| Stock by Size | Tracks purchased, sold, and remaining quantities for each size | Inventory |
| Sales Log | Records sales and calculates revenue, cost of goods sold, and profit | Sales and orders |
| Shipment Planner | Calculates reorder needs from remaining stock and sales | Purchasing and replenishment |
| Size Chart Generator | Produces a size chart from product measurements | Website size-chart data |
| Setup & Lists | Stores reusable lists such as brands, categories, and sizes | Reference/configuration data |
| Dashboard | Summarizes sales, stock, profit, and reorder information | Reports and dashboard queries/views |

### 1.2 Current operating workflow

```text
Register product
      |
      v
Record product-specific size measurements
      |
      v
Create purchase record for each product variant
      |
      v
Receive shipment and record actual quantities received
      |
      v
Add received quantities to inventory
      |
      v
Record sales against the exact product variant
      |
      v
Update stock and calculate available quantities
      |
      v
Review dashboard, low-stock alerts, and reorder suggestions
      |
      v
Publish approved products to the e-commerce website
```

### 1.3 Example of size-wise stock

A product can remain available even when one of its sizes is sold out. Therefore, its variants need separate inventory quantities.

| Product | Size | Received | Sold/issued | On hand |
|---|---:|---:|---:|---:|
| Embroidered Suit A | S | 5 | 2 | 3 |
| Embroidered Suit A | M | 8 | 5 | 3 |
| Embroidered Suit A | L | 4 | 4 | 0 |
| Embroidered Suit B | M | 6 | 1 | 5 |

The website should show that size L of Embroidered Suit A is unavailable while sizes S and M remain available.

---

## 2. Recommended Architecture

Use a centralized relational database (MySQL or PostgreSQL) with a backend/API layer that serves both the admin dashboard and the e-commerce frontend.

```text
                   CENTRAL DATABASE
   Products | Variants | Prices | Stock | Orders | Customers
                         |
                         v
                BACKEND / BUSINESS API
       Product publishing | Checkout | Reservations
       Inventory movements | Payments | Order processing
                    /           \
                   /             \
                  v               v
         ADMIN DASHBOARD     E-COMMERCE WEBSITE
         Manage products    Browse products
         Manage purchases   View variants and sizes
         Receive stock      Add to cart and checkout
         Manage orders      Track customer orders
         View reports       Submit reviews
```

### Why one database is recommended

- A product only needs to be registered once.
- The admin dashboard and website use the same product and variant records.
- Stock changes made by purchases, sales, returns, or adjustments are reflected consistently.
- Orders from the website and other channels can be recorded in one system.
- Historical sales and cost information remain centralized.
- There is no need to synchronize two independently maintained product databases.

The website should **not** connect directly to the database. It should access data through backend APIs so that permissions, stock checks, pricing rules, and checkout operations are enforced centrally.

---

## 3. Product Catalogue Database

This module stores the information required to identify, organize, publish, and display products.

### 3.1 Recommended tables

| Table | Important columns | Purpose |
|---|---|---|
| `brands` | `id`, `name`, `logo_url` | Brand information |
| `categories` | `id`, `name`, `parent_category_id` | Hierarchical categories and subcategories |
| `products` | `id`, `product_code`, `name`, `brand_id`, `category_id`, `description`, `material`, `status` | Main product information |
| `product_variants` | `id`, `product_id`, `sku`, `size_id`, `color_id`, `weight`, `selling_price`, `status` | Purchasable size/color combinations |
| `sizes` | `id`, `size_name` | Shared size definitions such as XS, S, M, L, XL |
| `colors` | `id`, `color_name`, `hex_code` | Shared color definitions |
| `product_images` | `id`, `product_id`, `image_url`, `sort_order`, `is_primary` | Product gallery images |
| `product_attributes` | `id`, `product_id`, `attribute_name`, `attribute_value` | Flexible details such as fabric, fit, care instructions, or pattern |

### 3.2 Product versus product variant

A **product** is the general item customers recognize. A **product variant** is a specific purchasable version of that item.

Example product:

- Product code: `GLI-001`
- Name: `ETHNC Embroidered Suit`
- Brand: `ETHNC`
- Category: `Embroidered Suit`
- Material: `Cotton blend`

Example variants:

| Variant SKU | Product | Size | Color |
|---|---|---|---|
| `GLI-001-S-BLK` | ETHNC Embroidered Suit | S | Black |
| `GLI-001-M-BLK` | ETHNC Embroidered Suit | M | Black |
| `GLI-001-L-BLK` | ETHNC Embroidered Suit | L | Black |

Each variant should have a unique SKU. Inventory, reservations, and order quantities should refer to the variant rather than only to the product.

If the product has only sizes and no colors, the color field can be optional. The model should still support additional options in the future.

### 3.3 Product publication status

Keep publication status separate from inventory quantity.

Suggested product statuses:

- `draft`: being prepared; not visible to customers.
- `pending_review`: waiting for approval.
- `published`: eligible to appear on the website.
- `hidden`: temporarily removed from public listings.
- `discontinued`: no longer offered for normal sale.

A published product can still be out of stock. Stock availability should be calculated from inventory, not represented by changing the product's publication status.

---

## 4. Size Measurement Management

The current workbook stores measurements in separate size columns. In a relational database, store each measurement as an individual row instead.

### Recommended table: `product_measurements`

Suggested columns:

- `id` — primary key
- `product_id` — foreign key to `products.id`
- `size_id` — foreign key to `sizes.id`
- `garment_section` — for example, Shirt or Trouser
- `measurement_name` — for example, Length, Chest, Body, Waist, or Sleeve
- `measurement_value` — numeric measurement
- `unit` — inch, cm, or another supported unit

Example:

| Product | Section | Measurement | Size | Value | Unit |
|---|---|---|---|---:|---|
| GLI-001 | Shirt | Length | S | 37 | inch |
| GLI-001 | Shirt | Length | M | 38 | inch |
| GLI-001 | Shirt | Body | S | 41 | inch |
| GLI-001 | Trouser | Length | M | 39 | inch |

This design supports products with different garment sections and different measurement types. The website can retrieve the measurements and generate the correct size chart for each product.

If measurements vary by color or by a specific variant, use `variant_id` instead of (or in addition to) `product_id` as the measurement reference.

---

## 5. Purchasing, Receiving, and Inventory Management

This is the most important operational module. Avoid relying only on manually editable `quantity_purchased` and `quantity_sold` fields. Use an auditable movement-based inventory system.

### 5.1 Recommended tables

| Table | Important columns | Purpose |
|---|---|---|
| `suppliers` | `id`, `name`, `contact_details`, `country` | Supplier and sourcing details |
| `purchase_orders` | `id`, `supplier_id`, `order_date`, `status`, `currency`, `expected_date` | Records purchase orders |
| `purchase_order_items` | `id`, `purchase_order_id`, `variant_id`, `quantity_ordered`, `unit_cost` | Variants and quantities ordered |
| `shipments` | `id`, `purchase_order_id`, `shipment_reference`, `dispatch_date`, `expected_arrival_date`, `status` | Tracks incoming shipments |
| `goods_receipts` | `id`, `shipment_id`, `warehouse_id`, `received_at`, `received_by` | Records stock physically received |
| `goods_receipt_items` | `id`, `goods_receipt_id`, `variant_id`, `quantity_received`, `quantity_rejected` | Records received quantity by variant |
| `warehouses` | `id`, `name`, `location` | Storage locations |
| `inventory_movements` | `id`, `variant_id`, `warehouse_id`, `movement_type`, `quantity`, `reference_type`, `reference_id`, `created_at` | Permanent history of stock changes |
| `inventory_balances` | `variant_id`, `warehouse_id`, `quantity_on_hand`, `updated_at` | Fast current-stock summary |
| `stock_reservations` | `id`, `order_item_id`, `variant_id`, `warehouse_id`, `quantity`, `status` | Stock allocated to customer orders |
| `inventory_reorder_rules` | `id`, `variant_id`, `warehouse_id`, `reorder_point`, `target_quantity` | Variant-specific replenishment rules |

### 5.2 How stock should be added

Example: the business orders 10 units of size M from a supplier.

1. Create a purchase order.
2. Add the M variant with an ordered quantity of 10.
3. Associate the purchase order with the incoming shipment.
4. When the shipment arrives, create a goods receipt.
5. Record the actual received quantity. It may be less than the quantity ordered.
6. For accepted goods, create an inventory movement of type `receipt`.
7. Update the corresponding inventory balance.

Do not increase stock merely because a purchase order was created. Stock should increase only when goods are physically received and accepted into inventory.

### 5.3 How stock should be reduced

Stock can leave inventory for several reasons:

- A customer order is dispatched.
- Goods are damaged or written off.
- Stock is transferred to another location.
- A manual adjustment is approved.
- Goods are returned to a supplier.

Each event should create an appropriate inventory movement with a reference to the transaction that caused it.

### 5.4 Stock calculation

Use the following concepts:

```text
Quantity on hand = accepted stock received
                   - stock dispatched or otherwise issued
                   + accepted customer returns
                   +/- approved adjustments and transfers

Available to sell = quantity on hand - active reserved quantity
```

`inventory_movements` should be the permanent audit history. `inventory_balances` is a summary used for fast reads. The balance must be updated transactionally whenever a movement is posted.

For multiple warehouses, calculate availability by variant and warehouse, then aggregate it where the website needs a combined total.

### 5.5 Inventory movement types

Suggested movement types include:

- `receipt`
- `sale_dispatch`
- `customer_return`
- `supplier_return`
- `damage_writeoff`
- `adjustment_in`
- `adjustment_out`
- `transfer_in`
- `transfer_out`

For transfers, create linked outgoing and incoming movements so that the total stock across warehouses remains consistent.

---

## 6. Sales, Orders, Payments, and Delivery

All sales channels should eventually use the same order-management system. This includes the website and any sales handled through Facebook, Instagram, WhatsApp, or physical retail.

### Recommended tables

| Table | Important columns | Purpose |
|---|---|---|
| `customers` | `id`, `name`, `email`, `phone` | Customer profiles |
| `customer_addresses` | `id`, `customer_id`, `address_type`, `address`, `city`, `postal_code` | Billing and delivery addresses |
| `sales_channels` | `id`, `name` | Website, Facebook, Instagram, WhatsApp, retail, etc. |
| `orders` | `id`, `customer_id`, `sales_channel_id`, `order_date`, `status`, `subtotal`, `discount_total`, `shipping_fee`, `grand_total` | Order header |
| `order_items` | `id`, `order_id`, `variant_id`, `quantity`, `unit_price`, `unit_cost`, `discount_amount` | Items and price/cost snapshots |
| `payments` | `id`, `order_id`, `payment_method`, `amount`, `status`, `transaction_reference` | Payment records |
| `order_status_history` | `id`, `order_id`, `old_status`, `new_status`, `changed_at` | Audit trail of order statuses |
| `delivery_shipments` | `id`, `order_id`, `courier`, `tracking_number`, `status`, `shipped_at`, `delivered_at` | Delivery and tracking |
| `product_reviews` | `id`, `customer_id`, `product_id`, `rating`, `review_text`, `status`, `created_at` | Customer feedback |

### Important financial rule: preserve price snapshots

Each `order_items` row should save the selling price and unit cost that applied when the order was placed. Do not rely on the product's current price to calculate historical revenue or profit. Product prices may change later, but past orders must remain financially accurate.

### Sales channel consistency

A sale recorded through WhatsApp should create an order just like a sale placed through the website. This ensures that every sales channel reduces the same inventory and contributes to the same sales reports.

---

## 7. How the E-commerce Website Uses the Database

The website should retrieve products from the central catalogue through backend APIs. It should not maintain a separate copy of product data that administrators must update manually.

### 7.1 Customer-facing product information

| Website feature | Database source |
|---|---|
| Product name and description | `products` |
| Brand | `brands` |
| Category and subcategory | `categories` |
| Product gallery | `product_images` |
| Selling price | `products` and/or `product_variants` |
| Sizes and colors | `product_variants`, `sizes`, `colors` |
| Size chart | `product_measurements` |
| Fabric, fit, and other details | `product_attributes` |
| Stock availability | `inventory_balances` and `stock_reservations` |
| Average rating and reviews | `product_reviews` |
| Related products | Product/category relationships or recommendation queries |

Choose one authoritative place for selling price. If variants can have different prices, store the price on `product_variants`; otherwise, a product-level default price can be used with an optional variant override. Define this rule clearly so the website and admin dashboard never disagree.

### 7.2 Product listing rules

The public product listing API should normally return products where:

- The product status is `published`.
- The product is not discontinued.
- The product is permitted for the relevant storefront or sales channel.

Stock status should be computed separately. A published product can be:

- In stock
- Low stock
- Out of stock
- Available for preorder (only if the business supports it)
- Coming soon

Do not hide an otherwise published product automatically just because one size is out of stock. The website can display the product and disable only the unavailable variant.

### 7.3 Typical product page

A normal e-commerce product page should show:

1. Product name, brand, and category.
2. Main image and image gallery.
3. Description and product attributes.
4. Price and any promotional price.
5. Available colors and sizes.
6. Variant-specific availability.
7. Product size chart.
8. Quantity selector.
9. Add-to-cart and checkout actions.
10. Reviews, ratings, and related products.

---

## 8. Online Checkout and Inventory Synchronization

The backend must re-check inventory during checkout. A stock quantity shown on a product page can become outdated if another customer purchases the same variant.

### Recommended order flow

```text
Customer selects product variant and quantity
                   |
                   v
             Add to cart
                   |
                   v
       Backend validates current stock
                   |
                   v
        Checkout creates an order
                   |
                   v
   Reserve requested variant quantities atomically
                   |
                   v
           Process payment
             /         \
            /           \
       Successful      Failed/expired
           |                |
           v                v
   Confirm order       Release reservation
           |
           v
   Prepare and dispatch order
           |
           v
   Create stock-out movement
           |
           v
   Website availability reflects new stock
```

### Reservation and dispatch distinction

A reservation reduces the quantity available for another customer to buy, but it does not mean the goods have physically left the warehouse.

- **At reservation:** increase reserved quantity; do not yet reduce physical on-hand stock.
- **At dispatch:** create the physical stock-out movement and complete/release the related reservation in the same database transaction.
- **At cancellation or reservation expiry:** release the reservation.
- **At return:** inspect the returned item. Restore stock only after it is accepted back into sellable inventory.

Use database transactions and row-level locking or an equivalent concurrency-safe mechanism when reserving stock. This prevents two customers from successfully buying the same last unit.

---

## 9. Reordering and Shipment Planning

The existing shipment planner should become a replenishment feature driven by variant-level data.

### Recommended inputs

- Current on-hand quantity.
- Active reserved quantity.
- Available quantity.
- Average sales velocity over a selected period.
- Reorder point for the specific variant.
- Target stock quantity.
- Quantity already ordered but not yet received.
- Expected arrival date of incoming shipments.
- Supplier lead time.
- Minimum supplier order quantity, if applicable.

### Simple starting calculation

```text
Available quantity = on-hand quantity - reserved quantity

Inventory position = available quantity + outstanding purchase quantity

Suggested reorder quantity = max(0, target stock quantity - inventory position)
```

This is a starting point. A more advanced model can account for expected demand during supplier lead time, seasonality, and safety stock.

Reorder rules should be configured per product variant because size S may sell at a different rate from size XL.

---

## 10. Recommended Database Relationships

The following is the main relationship model. `1:N` means one-to-many.

### 10.1 Product relationships

```text
BRANDS (1) -------------------- (N) PRODUCTS
CATEGORIES (1) ---------------- (N) PRODUCTS
CATEGORIES (1) ---------------- (N) CATEGORIES
PRODUCTS (1) ------------------ (N) PRODUCT_VARIANTS
PRODUCTS (1) ------------------ (N) PRODUCT_IMAGES
PRODUCTS (1) ------------------ (N) PRODUCT_ATTRIBUTES
PRODUCTS (1) ------------------ (N) PRODUCT_MEASUREMENTS
SIZES (1) --------------------- (N) PRODUCT_VARIANTS
COLORS (1) -------------------- (N) PRODUCT_VARIANTS
```

### 10.2 Purchasing and inventory relationships

```text
SUPPLIERS (1) ----------------- (N) PURCHASE_ORDERS
PURCHASE_ORDERS (1) ----------- (N) PURCHASE_ORDER_ITEMS
PRODUCT_VARIANTS (1) ---------- (N) PURCHASE_ORDER_ITEMS
PURCHASE_ORDERS (1) ----------- (N) SHIPMENTS
SHIPMENTS (1) ----------------- (N) GOODS_RECEIPTS
GOODS_RECEIPTS (1) ------------ (N) GOODS_RECEIPT_ITEMS
PRODUCT_VARIANTS (1) ---------- (N) GOODS_RECEIPT_ITEMS
PRODUCT_VARIANTS (1) ---------- (N) INVENTORY_MOVEMENTS
WAREHOUSES (1) ---------------- (N) INVENTORY_MOVEMENTS
PRODUCT_VARIANTS (1) ---------- (N) INVENTORY_BALANCES
WAREHOUSES (1) ---------------- (N) INVENTORY_BALANCES
PRODUCT_VARIANTS (1) ---------- (N) STOCK_RESERVATIONS
PRODUCT_VARIANTS (1) ---------- (N) INVENTORY_REORDER_RULES
```

### 10.3 Customer and order relationships

```text
CUSTOMERS (1) ------------------ (N) CUSTOMER_ADDRESSES
CUSTOMERS (1) ------------------ (N) ORDERS
SALES_CHANNELS (1) ------------- (N) ORDERS
ORDERS (1) --------------------- (N) ORDER_ITEMS
PRODUCT_VARIANTS (1) ----------- (N) ORDER_ITEMS
ORDERS (1) --------------------- (N) PAYMENTS
ORDERS (1) --------------------- (N) ORDER_STATUS_HISTORY
ORDERS (1) --------------------- (N) DELIVERY_SHIPMENTS
ORDER_ITEMS (1) --------------- (N) STOCK_RESERVATIONS
CUSTOMERS (1) ------------------ (N) PRODUCT_REVIEWS
PRODUCTS (1) ------------------- (N) PRODUCT_REVIEWS
```

### Relationship rules

- Every table should have a primary key.
- Use foreign keys for references between tables.
- Use stable IDs rather than product names as references.
- Give each product variant a unique SKU.
- Add unique constraints where appropriate, such as a unique SKU and a unique `(product_id, size_id, color_id)` combination when that combination is intended to identify one variant.
- Use appropriate indexes on foreign keys and common lookup fields such as SKU, product status, category, order status, and movement date.
- Use decimal data types for money, not floating-point data types.
- Use appropriate deletion rules. Historical orders and inventory movements should not disappear because a product is hidden or discontinued.

---

## 11. Issues to Resolve in the Existing Workbook

The workbook is a useful operational prototype, but its data should be cleaned before migration.

### 11.1 Product ID consistency

The Product Master contains an ID such as `GLI-2606-ETH-EMS-001`, while the Stock by Size and Size Measurements sheets contain IDs such as `GLI-2609-ETH-PRS-003` and `GLI-2609-ETH-PNS-001`.

These identifiers do not match. Related stock and measurement records must be mapped to the correct product or variant before import. Do not silently assume they refer to the same product.

### 11.2 Shipment and receiving history

A purchased quantity should not automatically be treated as available stock. The migrated system should distinguish:

- Quantity ordered.
- Quantity dispatched by supplier.
- Quantity received.
- Quantity rejected or damaged.
- Quantity accepted into inventory.
- Quantity still outstanding.

### 11.3 Size-specific reorder logic

Reorder recommendations should be generated for each variant, rather than using only the overall product quantity. Different sizes can have different sales rates and minimum stock requirements.

### 11.4 Formula issue

The Shipment Planner contains a broken `#REF!` formula at cell `C63`. Review this during workbook cleanup and migration.

### 11.5 Migration validation

Before importing the workbook, validate that:

- Every stock record points to a valid product variant.
- Every measurement points to a valid product and size.
- Product codes and SKUs are unique and consistently formatted.
- Quantities are non-negative unless a specific adjustment process permits otherwise.
- Historical sales have valid product references, dates, quantities, prices, and costs.
- Summary totals reconcile with the original workbook where the original formulas and records are valid.

---

## 12. Final Recommended Table List

This is the proposed implementation checklist. It can be split into phases rather than built all at once.

| Module | Tables |
|---|---|
| Product catalogue | `brands`, `categories`, `products`, `product_variants`, `sizes`, `colors`, `product_images`, `product_attributes` |
| Size management | `product_measurements` |
| Suppliers and purchasing | `suppliers`, `purchase_orders`, `purchase_order_items` |
| Import and receiving | `shipments`, `goods_receipts`, `goods_receipt_items` |
| Inventory | `warehouses`, `inventory_movements`, `inventory_balances`, `stock_reservations`, `inventory_reorder_rules` |
| Sales and orders | `customers`, `customer_addresses`, `sales_channels`, `orders`, `order_items`, `payments`, `order_status_history` |
| Delivery | `delivery_shipments` |
| Customer experience | `product_reviews`, `wishlists`, `wishlist_items`, `carts`, `cart_items` |
| Business configuration | `settings` and other configurable reference tables |
| Reporting | Database views or reporting queries for sales, stock, profit, and shipment dashboards |

Some tables, such as carts, wishlists, and reviews, are required only when those features are part of the e-commerce scope. Reporting tables generally do not need to duplicate transactional data; use database views or carefully maintained reporting summaries where appropriate.

---

## 13. Suggested Implementation Phases

### Phase 1 — Core product catalogue

Build brands, categories, products, variants, sizes, colors, images, and product attributes. Clean and import the Product Master data. Establish unique product codes and variant SKUs.

### Phase 2 — Inventory foundation

Build suppliers, purchase orders, shipments, goods receipts, warehouses, inventory movements, inventory balances, and stock reservations. Import validated opening stock and retain an opening-stock audit reference.

### Phase 3 — Internal operations and reporting

Build the admin workflows for purchasing, receiving, stock adjustments, sales recording, reorder rules, and the dashboard. Reconcile the new reports against the workbook.

### Phase 4 — E-commerce catalogue

Build public product listing and detail APIs. Add publication approval, product images, variant selection, size charts, availability, search, filtering, and sorting.

### Phase 5 — Checkout and order fulfillment

Build carts, orders, order items, payments, reservations, delivery tracking, cancellations, and returns. Ensure all sales channels use the same inventory and order services.

### Phase 6 — Advanced operations

Add more advanced demand forecasting, supplier lead-time analysis, multi-warehouse transfers, promotional pricing, returns analytics, and business intelligence as needed.

---

## 14. Final Recommendation

Build **one centralized inventory and product-management backend** and expose it through APIs to both the admin dashboard and the e-commerce website.

The product should be registered once. Administrators can complete its details, add variants and measurements, upload images, set prices, and submit it for publication. Once approved and published, the website can display it automatically.

Every purchase, shipment receipt, sale, reservation, cancellation, stock adjustment, and accepted return should update the same inventory system. This creates a consistent source of truth for the entire business.

The most important architectural decisions are:

1. Track stock by product variant (size/color/SKU), not only by product.
2. Record stock changes as inventory movements with an audit trail.
3. Separate physical on-hand stock from reserved and available-to-sell stock.
4. Preserve historical prices and costs on order items.
5. Keep product publication status separate from stock availability.
6. Use one backend and one product/inventory database for every sales channel.
7. Make checkout reservations concurrency-safe so the same stock cannot be sold twice.

With these foundations, the system can support the current Glintora workflow while remaining flexible enough to grow into a full e-commerce and multi-channel inventory platform.
