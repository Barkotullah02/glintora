# Glintora Admin — User Guide

A practical guide for running the Glintora store from the admin dashboard: catalogue,
inventory, purchasing and sales. Every function in the app is explained briefly below.

---

## 1. Logging in

1. Go to **http://localhost:8080/admin** (while the app is running).
2. Sign in with your admin account:

   | Field | Value (demo seed) |
   |---|---|
   | Email | `admin@glintora.com` |
   | Password | `admin123` |

3. You land on the **Dashboard**. Only admins can open `/admin` — customers never see this area.

---

## 2. How the dashboard is laid out

| Element | What it is |
|---|---|
| **Left sidebar** | Main menu, grouped into **Overview · Catalog · Inventory · Sales**. Groups collapse when clicked. |
| **Sidebar badges** | Small numbers next to *Product Reviews* (pending reviews), *Reorder Planning* (suggestions waiting), *Orders* (orders awaiting action). |
| **Top bar** | Site search box, your user menu and **Log out**. |
| **Breadcrumbs** | Path shown under the top bar (e.g. `Dashboard / Inventory / Reorder planning`) — click any level to go back. |
| **Flash messages** | Green/red banners at the top of the page confirming an action or explaining an error. They disappear on their own. |
| **Mobile** | On phones the sidebar becomes a slide-out menu (hamburger button) and a bottom navigation bar appears with Dashboard, Inventory and Orders. |

Destructive actions (cancel order, cancel purchase order, delete variant) always ask for
confirmation first.

---

## 3. Overview — Dashboard (`/admin`)

The daily health check. Everything on it responds to the **date-range selector (7 / 30 / 90 days)**.

| Section | Function |
|---|---|
| **Revenue** | Total sales in the selected period, with a comparison against the previous period (↑/↓ %). |
| **Orders** | Number of orders placed in the period. |
| **Inventory value** | What your stock is worth **at cost** — what you paid suppliers, not the sale price. |
| **Stock alerts** | Count of variants that are **low** or **out of stock** — click through to fix them. |
| **Sales chart** | Bar chart of revenue per day for the selected period; hover a bar for the exact value. |
| **Inventory health** | Split of variants that are healthy / low / out of stock. |
| **Needs your attention** | A short to-do list: low-stock variants, overdue shipments, orders waiting for you. |
| **Recent orders** | The latest orders with payment + fulfilment status — click a row to open it. |
| **Recent activity** | Latest stock movements (who changed what and why). |

---

## 4. Catalog

### 4.1 Products (`/admin/products`)

The product list and editor.

**Summary indicators** at the top: Total · Published · Draft · Pending review · Out of stock.

**Filters:** free-text search (`q`), **Category**, **Brand**, **Status** (published / draft / discontinued / hidden).

**Columns:** Product (name + SKU/code) · Brand · Category · Variants (count) · Price · **Available stock** · Publication status.

| Function | How to use it |
|---|---|
| **Add product** | *New product* button — fill name, product code, brand, category, prices, description, then save. New products start as **draft**. |
| **Edit product** | *Edit* on any row — change details, price, description. |
| **Variants** | On the edit page, add variants (size/colour combinations). Each variant has its own SKU and price. |
| **Delete variant** | *Delete* on the variant row (confirm dialog). |
| **Images** | On the edit page, add/remove product images. |
| **Publish / unpublish** | Status button on the edit page. Only **published** products appear on the storefront. |
| **Stock** | ⚠️ You **cannot** type stock here on purpose. Stock only changes through Inventory (receipts, adjustments, sales) so the audit trail stays honest. |

### 4.2 Categories (`/admin/categories`)

Lists every category with how many products use it.

- **Add category** — name plus an optional **parent** to nest it (Women → Women → Dresses).
- Categories organise the storefront menus; a product must have one to be findable.

### 4.3 Brands (`/admin/brands`)

Lists brands with their logo and product count.

- **Add brand** — name and optional logo URL. Add the brand first, then assign it to products.

### 4.4 Product Reviews (`/admin/reviews`)

Customer reviews waiting for moderation — **nothing goes public until you approve it**.

- **Tabs:** All · Pending · Approved · Rejected (counts shown on each tab).
- Each row shows stars, review text, customer, product and status.
- **Actions per review:** **Approve** (appears on the storefront), **Reject** (hidden), **Reset** (back to pending).

---

## 5. Inventory

### 5.1 Stock Overview (`/admin/inventory`)

Variant-level stock, one row per variant **per warehouse**.

**Filters:** search (`q`), **Status** (in / low / out), **Warehouse**.

**The four numbers — memorise these:**

| Term | Meaning |
|---|---|
| **On hand** | Physical units sitting in that warehouse. |
| **Reserved** | Units held for confirmed orders that haven't shipped yet — you can't sell them twice. |
| **Available** | `on hand − reserved` — what you can actually promise a customer. |
| **Incoming** | Units on purchase orders you've ordered but not received yet. |

**Actions on this page:**

| Function | What it does |
|---|---|
| **Receive stock** | Log stock that arrived *outside* the purchase-order flow (e.g. a manual delivery or correction): pick variant, warehouse, **quantity received** (optionally also **rejected** units and who received them) → creates a stock receipt movement. |
| **Adjust stock** | Correct a count: enter a **delta** (positive or negative) and a **reason**. The **projected quantity** is previewed before you save. Every adjustment is written to the movement log with your name and the reason. |

### 5.2 Stock Movements (`/admin/movements`)

The **append-only** audit trail — every unit that ever entered or left stock.

- **Filters:** movement **type** (receipt, sale, adjustment, return, etc.) and **warehouse**.
- Each row shows date, variant, type, quantity change, the resulting balance, **who did it** and **why**.
- Entries are **never edited or deleted** — corrections are made with a new adjustment, not by rewriting history.

### 5.3 Purchase Orders (`/admin/purchase-orders`)

Buying stock from suppliers.

**Status tabs:** All · Draft · Ordered · Partial · Received · Cancelled.

| Function | What it does |
|---|---|
| **New purchase order** | Choose a supplier, an optional expected date, then add lines: variant, quantity, unit cost. Saves as a **draft** — nothing is ordered yet. |
| **Send to supplier** | Draft → **Ordered**. This marks the PO as officially placed. |
| **Book shipment** | On the PO detail page, record an incoming shipment: reference, dispatch date, expected arrival. |
| **Cancel order** | Draft/ordered → **Cancelled** (cannot be undone). |
| **PO detail** | Shows supplier, status, expected date, line items (ordered/received/remaining), total value, shipments and goods receipts. |

**Lifecycle:** `draft → ordered → (partial) → received`, with `cancelled` possible before receiving.
**Partial** means some units arrived and some are still outstanding.

### 5.4 Suppliers (`/admin/suppliers`)

- **Add supplier** — name, country, contact details.
- The list shows who you buy from; a supplier must exist before you can create a purchase order.

### 5.5 Incoming Shipments (`/admin/shipments`)

Tracks parcels ordered from suppliers.

**Lifecycle (use the buttons on each row to advance):**

```
Ordered → Dispatched → In Transit → Arrived → Receiving → Completed
   └────────────────── cancelled (before receiving) ──────────┘
```

| Function | What it does |
|---|---|
| **Status buttons** | Only the *legal next steps* for the current state are shown, so you can't skip stages. |
| **Overdue filter** | Show only shipments whose expected arrival has passed — chase these first. |
| **Book shipment** | Also available from the purchase-order detail page (see 5.3). |

### 5.6 Goods Receiving (`/admin/receiving`)

Turning a delivered shipment into stock.

1. Pick the **incoming shipment** and the **warehouse** it lands in.
2. The table shows one line per ordered variant: **ordered · already received · remaining**.
3. Enter **received** (and optionally **rejected**) quantities per line — you can receive **partially**; leave lines at 0 if they didn't arrive.
4. **Post receipt** — accepted units are added to stock, a movement is written with you as the actor, and the system automatically:
   - sets the shipment to *Receiving* (or *Completed* if everything arrived), and
   - sets the PO to *Partial* (or *Received* if all lines are complete).

### 5.7 Warehouses (`/admin/warehouses`)

Read-only overview per warehouse: variants stocked, units on hand, **value at cost**, and how many
rows are low/out of stock. Stock always belongs to exactly one warehouse.

### 5.8 Reorder Planning (`/admin/reorder`)

An automatic shopping list for restocking.

- A suggestion appears whenever a variant falls to its **reorder point**.
- **Suggested quantity = `max(0, target − (available + incoming))`** — it already accounts for stock you can sell and stock already on order, so you never double-order.
- **Create draft PO** on each row: pick a supplier (quantity is pre-filled with the suggestion) → creates a **draft** purchase order. You review and send it yourself — nothing is ever ordered automatically.

---

## 6. Sales

### 6.1 Orders (`/admin/orders`)

**Status tabs with counts:** All · Pending · Confirmed · Processing · Shipped · Delivered · Cancelled · Returned.
**Search:** order number, customer name or e-mail.

**Columns:** Order · Customer · Date · Total · **Payment status** · **Fulfilment status**.

> Payment and fulfilment are tracked **separately**: an order can be paid but not yet shipped, or shipped on cash-on-delivery and still awaiting payment.

| Function | When it's available | What it does |
|---|---|---|
| **Confirm** | Pending | Confirms the order and **reserves** its stock. |
| **Start processing** | Confirmed | Moving it to the warehouse queue. |
| **Dispatch (deduct stock)** | Confirmed / Processing | Enter the courier name, then ship it — this is the moment **stock is deducted** and reservations are released. |
| **Mark delivered** | Shipped | Order complete. |
| **Cancel order** | Not yet shipped/delivered/returned | Cancels it and **releases the reserved stock** back to available. Asks for confirmation. |

**Order detail page** contains:

- **Items** — each line with quantity, unit price, unit cost and line total, plus subtotal/shipping/discount/grand total.
- **Timeline** — every status change with date and old → new status.
- **Customer & delivery** — contact details and shipping address.
- **Payments** — each payment with method, status (paid / pending / failed / refunded) and amount.
- **Delivery** — courier, tracking number, shipped/delivered timestamps.

### 6.2 Customers (`/admin/customers`)

- **Search** by name or e-mail.
- Per customer: phone, **orders** count, **total spend**, **last order** date and member-since date — use it to spot your regulars.

---

## 7. Status reference (quick lookup)

| Domain | States |
|---|---|
| **Product** | `draft → published` (also `discontinued`, `hidden`) |
| **Purchase order** | `draft → ordered → partial → received`, or `cancelled` |
| **Shipment** | `ordered → dispatched → in_transit → arrived → receiving → completed`, or `cancelled` |
| **Order** | `pending → confirmed → processing → shipped → delivered`, or `cancelled` / `returned` |
| **Payment** | `pending · paid · failed · refunded` |
| **Review** | `pending → approved / rejected` |
| **Stock row** | `in` (healthy) · `low` (at/below reorder point) · `out` (zero available) |

---

## 8. Good habits

1. **Publish deliberately** — keep new products in draft until photos, prices and variants are complete.
2. **Never hand-edit stock in the product editor** — use *Receive stock* or *Adjust stock* so every change has a reason and an actor.
3. **Advance shipments with the buttons** — they only offer legal next steps; the overdue filter is your daily chase list.
4. **Receive partially when needed** — record what actually arrived; the remainder stays visible as *remaining*.
5. **Reorder from the Reorder Planning page** — it prevents over-ordering; always review the draft PO before sending it.
6. **Approve reviews before they go public** — the pending badge in the sidebar tells you when any are waiting.
7. **Check the Dashboard daily** — the *Needs your attention* card collects low stock, overdue shipments and waiting orders in one place.
