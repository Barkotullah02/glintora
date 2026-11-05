# Glintora Admin Dashboard
## UI/UX Specification and Development Blueprint

**Purpose:** A practical, implementation-ready guide for building Glintora's responsive admin dashboard for product catalogue management, inventory, purchasing, shipments, orders, and e-commerce operations.

---

## 1. Product Goal

Build a **minimal, premium, operationally clear admin application**. The customer-facing storefront should feel editorial and fashion-focused; the admin should feel like a polished SaaS tool optimized for speed, accuracy, and everyday tasks.

Every page should help staff answer:
1. What is happening?
2. What requires attention?
3. What action can I safely take?

### Core principles
- Keep layouts calm and uncluttered.
- Make primary actions obvious.
- Use consistent tables, forms, status badges, and filters.
- Make desktop, tablet, and mobile first-class experiences.
- Keep business logic and permissions in the backend.
- Treat inventory as a transaction history, not a manually edited number.
- Use the same API and product/inventory source of truth for the admin and storefront.

---

## 2. Design System

### 2.1 Colors

| Token | Hex | Usage |
|---|---|---|
| `--color-page` | `#F8F7F4` | Main app background |
| `--color-surface` | `#FFFFFF` | Cards, tables, dialogs |
| `--color-text` | `#252525` | Primary text |
| `--color-muted` | `#777777` | Secondary text |
| `--color-border` | `#E8E5DF` | Borders and dividers |
| `--color-accent` | `#87917C` | Restrained highlights |
| `--color-success` | `#357A52` | Successful states |
| `--color-warning` | `#A86B16` | Warnings |
| `--color-danger` | `#B33A3A` | Errors and critical states |

Use semantic colors consistently. Do not give every card a different bright color. Use charcoal buttons with white text for the primary action.

### 2.2 Typography

Use **Inter** throughout the admin interface.

| Element | Desktop | Mobile |
|---|---:|---:|
| Page title | 28–32px | 24–28px |
| Section heading | 20–24px | 18–20px |
| KPI value | 28–36px | 24–28px |
| Table header | 12–13px | 12px |
| Table content | 13–14px | 13–14px |
| Body text | 14–15px | 14px |
| Form label | 13–14px | 13–14px |
| Helper text | 12px | 12px |
| Button | 13–14px | 13–14px |

### 2.3 Spacing

Use an 8px-based spacing scale:
- `4px`: tiny adjustments
- `8px`: icon/text gaps
- `12px`: compact field/card spacing
- `16px`: standard control and mobile page padding
- `24px`: card padding and section spacing
- `32px`: major content group separation
- `40–48px`: large section spacing

Desktop: sidebar around `240–260px`, top bar `64–72px`, content max width around `1600px`, page padding `24–32px`.

Mobile: `16px` page padding, single-column forms, compact page headers, and full-screen drawers for complex tasks.

### 2.4 Component appearance
- Card radius: `8–12px`
- Inputs/buttons: consistent height around `40–44px`; touch targets around `44–48px`
- Borders: subtle and consistent
- Shadows: minimal
- Icons: simple outline icons from one icon set
- Animation: restrained transitions around `150–200ms`
- Avoid excessive gradients, glassmorphism, oversized pill buttons, and decorative charts.

---

## 3. Application Shell and Navigation

### Desktop layout
- Persistent or sticky left sidebar.
- Top bar with global search, notifications, warehouse selector where relevant, and user menu.
- Scrollable main content area.
- Breadcrumbs on deep pages.
- Page title and primary action consistently placed.

### Navigation groups

```text
OVERVIEW
  Dashboard

CATALOG
  Products
  Categories
  Brands
  Collections
  Product Reviews

INVENTORY
  Stock Overview
  Stock Movements
  Purchase Orders
  Suppliers
  Incoming Shipments
  Goods Receiving
  Warehouses
  Stock Transfers
  Reorder Planning

SALES
  Orders
  Customers
  Returns & Refunds
  Payments

ECOMMERCE
  Storefront Publishing
  Homepage & Collections
  Promotions & Discounts

REPORTS
  Sales Analytics
  Inventory Reports
  Profitability
  Purchasing & Supplier Reports

ADMINISTRATION
  Staff & Permissions
  Activity Logs
  Settings
```

Implement navigation groups as collapsible sections. Use permission-aware navigation, but remember that hiding a menu item is not security; the API must enforce permissions too.

### Mobile layout
- Replace the sidebar with a slide-out drawer.
- Use a compact top bar.
- Optional bottom navigation: `Home`, `Inventory`, `Orders`, `More`.
- Use mobile record cards instead of forcing wide tables to scroll.
- Use bottom sheets or full-screen drawers for filters and complex forms.
- Never make essential actions available only on hover.

---

## 4. Dashboard

### Header
- Title: `Dashboard`
- Short description: `Your business at a glance.`
- Date range selector
- Warehouse selector, where applicable
- Export action if the user has permission

### KPI cards
1. Total revenue for selected period
2. Total orders
3. Inventory value with the valuation basis stated
4. Low-stock/out-of-stock variant alerts

Each card includes a label, value, optional comparison with the previous equivalent period, and a link to the detailed view.

### Main sections
- Sales performance chart: 7 days, 30 days, 90 days, custom range
- Inventory health: in-stock, low-stock, out-of-stock, reserved, incoming
- Needs Your Attention: pending approvals, shipments awaiting receipt, reorder alerts
- Recent orders
- Recent activity

Use live API values, not hardcoded demo metrics in production. Explain the reporting period and definitions. Inventory KPIs normally represent current state, even when the sales date range changes.

### Responsive behavior
- Desktop: four KPI cards per row where space permits
- Tablet: two cards per row
- Mobile: stacked or horizontally browsable KPI cards
- Keep inventory alerts and pending actions near the top on mobile

---

## 5. Product Catalogue

### Product listing
Header: `Products` with a primary `Add Product` button.

Summary indicators:
- Total products
- Published
- Draft
- Pending review
- Out of stock

Search by product name, product code, SKU, and brand. Filters should include category, brand, publication status, and stock status. Support sorting, pagination, bulk selection, and authorized bulk actions.

Suggested desktop columns:

| Product | Brand | Category | Variants | Price | Available stock | Publication |
|---|---|---|---:|---:|---:|---|

Use a thumbnail and compact text. On mobile show product cards with name, SKU/code, status, price, available stock, and a `View` action.

### Add/edit product form
Organize into sections or a stepper:
1. **Basic information:** name, unique product code, brand, category, description, fabric/material, tags.
2. **Images:** primary image, gallery, reorder, preview, remove, alt text.
3. **Variants:** size, color, unique SKU, optional barcode, weight, price overrides.
4. **Pricing and cost:** selling price, cost price, promotional price, currency.
5. **Measurements:** garment section, measurement name, size, value, unit.
6. **Publication:** draft, pending review, published, hidden, discontinued.

Use searchable dropdowns for brands and categories. On mobile, stack fields vertically. Keep labels persistent and validation messages next to fields.

**Important:** Do not freely edit stock quantities in the product editor. Opening stock and later changes must be recorded through an authorized inventory transaction.

### Variant model
A parent product may have variants such as size/color combinations. Each variant has a unique SKU and its own stock balance.

Allow users to generate size/color combinations, but let them disable combinations that do not exist. A product may be published while one or more variants are out of stock.

### Product detail page
Use tabs:
- Overview
- Variants & Stock
- Measurements
- Sales
- Activity

Show product information, image, publication state, prices, variant stock summaries, recent sales, and a history of important changes.

---

## 6. Inventory Management

### Stock overview
Display one row per variant, not merely one row per parent product.

Suggested columns:

| Product | SKU | Size/Color | Warehouse | On hand | Reserved | Available | Status |
|---|---|---|---|---:|---:|---:|---|

Definitions:
- **On hand:** physical quantity recorded at the location, including stock that may be non-sellable.
- **Reserved:** quantity allocated to orders but not yet dispatched.
- **Available:** sellable on-hand quantity minus reservations and other non-sellable allocations.
- **Incoming:** expected supply from outstanding purchases; it is not available for sale until received and accepted.

If damaged or quarantined stock is physically present, track it separately. Define available stock consistently in the API and storefront.

### Filters
- Product, SKU, brand, category
- Size and color
- Warehouse
- In stock, low stock, out of stock
- Reorder required

Mobile stock cards should show product/SKU, variant, on-hand, reserved, available, status, and a `View Details` action.

### Stock movements
Every stock change must create an auditable movement containing:
- Timestamp
- Product variant
- Warehouse
- Movement type
- Quantity delta
- Reference document
- Actor
- Reason/notes

Movement types may include opening stock, receipt, dispatch, accepted return, damage/loss, adjustment, and transfer.

Treat posted movements as append-only under normal application permissions. Correct errors through a controlled reversal or compensating movement rather than silently editing history.

### Stock adjustments
Require warehouse, variant, adjustment type, quantity, reason, and optional supporting note/document. Show current and projected quantities before confirmation. Enforce permissions and validation on the server.

### Reorder planning
Configure reorder thresholds and target stock per variant and, where needed, per warehouse.

Show current available stock, threshold, incoming quantity, recent sales velocity, suggested reorder quantity, preferred supplier, and expected replenishment date if known.

A simple starting formula is:

`Suggested reorder = max(0, Target stock - Available stock - Incoming stock)`

A stronger recommendation also considers demand during supplier lead time and safety stock. Suggestions should create draft purchase orders, not automatically place confirmed orders.

---

## 7. Purchasing, Suppliers, and Shipments

### Supplier management
List supplier name, country, contact, active status, outstanding orders, and purchase history. Supplier detail pages should show contact data, orders, shipments, and relevant notes.

### Purchase orders
Fields:
- Supplier
- Order date and expected date
- Currency
- Receiving warehouse
- Payment terms and notes
- Product variant, SKU, quantity, unit cost, line total
- Applicable additional costs

Support draft, submitted, approved, partially received, received, cancelled, and closed states as appropriate.

A purchase order records what was ordered. It must **not** increase sellable stock.

### Incoming shipments
A purchase order and a shipment are separate records. One order may arrive in multiple shipments. If the business permits consolidated shipments, one shipment may contain items from multiple orders.

Show shipment reference, related purchase orders, origin/destination, tracking reference, dispatch date, expected arrival, quantities expected and received, and status.

Suggested timeline:

`Ordered → Dispatched → In Transit → Arrived → Receiving → Completed`

### Goods receiving
For each variant, show:
- Ordered quantity
- Previously received quantity
- Quantity in this delivery
- Accepted quantity
- Rejected/damaged quantity
- Remaining quantity

Support partial receipts. Finalizing a receipt should create inventory movements for accepted quantities and separately record rejected quantities and their disposition.

Posted receipts should be corrected through controlled reversals or adjustments, not silently edited.

---

## 8. Warehouses and Transfers

Support multiple warehouses from the start, even if Glintora initially uses only one.

Warehouse overview:
- Name and location
- Number of stocked variants
- Physical stock and valuation
- Low-stock variants
- Pending transfers
- Active/inactive state

Warehouse detail tabs:
- Overview
- Inventory
- Receiving
- Transfers
- Activity

### Transfer workflow
`Draft → Approved (if required) → Dispatched → In Transit → Received → Reconciled`

Once dispatched, quantity should no longer be sellable at the source. In-transit quantity must not be counted as available at the destination until it is received and accepted. Record discrepancies and the responsible staff member.

---

## 9. Order Management

### Order list
Show:
- New orders
- Awaiting payment
- Processing
- Ready for dispatch
- Shipped
- Cancelled or requiring attention

Suggested columns:

| Order ID | Customer | Date | Total | Payment status | Fulfillment status |
|---|---|---|---:|---|---|

Search and filter by order ID, customer, date, status, payment, fulfillment, sales channel, and warehouse. Use compact cards on mobile.

### Order detail
Sections:
1. Order summary and statuses
2. Items, SKUs, sizes, colors, quantities, and price snapshots
3. Delivery address and courier/tracking
4. Payments and refunds
5. Chronological order timeline
6. Authorized actions

Possible actions include confirm, cancel, generate invoice, update fulfillment, add tracking, initiate eligible refund, and create return authorization. Actions must depend on the current state and user permission.

### Inventory and payment safety
- Validate and reserve stock atomically at checkout.
- Prevent two buyers from purchasing the same final unit.
- Release reservations on eligible cancellations.
- Deduct physical stock through a movement at dispatch, according to the defined fulfillment policy.
- Handle payment callbacks and retries idempotently.
- Never let duplicate requests create duplicate orders, payments, or movements.
- The backend calculates final prices and validates promotions; never trust client-submitted totals.

---

## 10. Returns and Refunds

List return reference, original order, customer, items, reason, request date, return status, and refund status.

Return detail should record quantity, reported condition, inspection outcome, refund amount, inventory disposition, and responsible staff member.

Typical workflow:

`Requested → Approved/Rejected → In Transit → Received → Inspecting → Accepted/Rejected → Completed`

Adapt states to the real process.

**Approval alone must not restock an item.** Only inspected and accepted sellable items should return to available inventory. Damaged or uninspected items should remain non-sellable or quarantined. Track refund requested, initiated, and confirmed separately.

---

## 11. Storefront and Merchandising

### Publication management
Show product image, name, brand, category, price, publication status, missing required data, and last updated date.

Authorized actions:
- Publish/unpublish
- Preview public page
- Schedule publication if supported
- Validate required fields
- Open the live page

Keep publication state separate from inventory state. An out-of-stock product can remain published if that is the business policy.

### Homepage management
Manage:
- Hero banners
- New arrivals
- Featured brands
- Trending collections
- Bestsellers
- Category highlights
- Promotional campaigns

For each section, support title, subtitle, image, destination, product/collection selection, ordering, active status, and optional schedule. Start with forms and preview; a complex drag-and-drop builder is not needed for the first release.

### Categories vs collections
Categories define taxonomy (for example, Ethnic Wear). Collections are merchandising groups (for example, Festive Edit or Bestsellers). A product can belong to one category and appear in several collections.

### Promotions
Support eligible products/collections, promotional prices, start/end dates, coupon rules, and usage limits. Record price changes and enforce pricing logic on the server.

---

## 12. Customers, Reports, and Settings

### Customers
List customer identifier, name, order count, total historical spend, last order, and account status. Detail tabs: Overview, Orders, Addresses, Returns, Activity.

Restrict personal data by role. Never expose passwords or authentication tokens.

### Reports
Recommended reports:
- Sales and orders
- Inventory and stock movements
- Product and size performance
- Purchasing and supplier spend
- Shipment delays and receiving discrepancies
- Inventory valuation
- Gross profitability

Define revenue, discounts, returns, COGS, and inventory valuation consistently. Clearly distinguish cost valuation from potential retail value. Provide date filters, warehouse/product filters, readable tables, and CSV/Excel export where appropriate.

### Settings
Organize into:
- General: business details, currency, timezone, formats
- Inventory: default warehouse, reservation rules, reorder defaults, negative-stock policy
- Purchasing: numbering, approval rules, terms
- Orders: numbering, reservation expiry, cancellation/fulfillment rules
- Notifications: low-stock, delayed shipment, approval and payment alerts
- Storefront: policies, contact details, default sorting
- Staff and permissions
- Activity logs

Critical configuration changes should be logged and confirmed where appropriate.

---

## 13. Staff Roles and Permissions

Use role-based access control (RBAC), enforced by the backend.

Suggested starting roles:

| Role | Main responsibilities |
|---|---|
| Super Admin | Full authorized system access |
| Inventory Manager | Stock, receiving, transfers, adjustments |
| Purchasing Manager | Suppliers, purchase orders, purchasing reports |
| Sales Manager | Orders, customers, returns, sales reports |
| Catalogue Manager | Products, images, categories, publishing |
| Warehouse Staff | Receiving, picking, dispatch, permitted stock operations |
| Finance Staff | Payments, refunds, costs, financial reports |
| Viewer | Read-only access to permitted information |

Separate create, approve, and execute permissions for sensitive actions. Consider dual approval for large stock adjustments or high-value purchase orders.

Record permission changes and important actions in the audit log. Hiding a button in the UI is not security; the API must reject unauthorized requests.

---

## 14. Notifications and Activity Log

Notifications should be actionable, not noisy.

Examples:
- Variant below reorder threshold
- Shipment overdue
- Purchase order awaiting approval
- Payment confirmation failed
- Product rejected during review
- Stock discrepancy recorded

Each notification should have a title, short explanation, timestamp, severity, and link to the affected record. Support read/unread state and sensible deduplication.

Activity logs should record actor, timestamp, action, affected entity, reference, and appropriate before/after values for critical changes. Never log passwords, tokens, or unnecessary personal data.

---

## 15. Responsive Design Rules

| Feature | Desktop | Tablet | Mobile |
|---|---|---|---|
| Navigation | Persistent sidebar | Collapsible sidebar | Drawer; optional bottom navigation |
| KPI cards | Up to four per row | Two per row | Stacked or horizontally browsable |
| Data tables | Full table | Reduced columns | Focused record cards |
| Forms | Two columns where useful | Adaptive | One column |
| Filters | Toolbar/sidebar | Collapsible | Drawer or full-screen panel |
| Dialogs | Centered modal | Responsive modal | Bottom sheet/full screen |
| Product gallery | Multi-column | Compact gallery | Swipeable gallery |
| Primary actions | Page header | Page header | Visible, easy-to-reach action |

Additional rules:
- Aim for 44–48px touch targets.
- Never rely on hover for essential actions.
- Use persistent form labels and field-level validation.
- Warn before destructive operations.
- Protect against accidental loss of unsaved work.
- Provide loading skeletons, empty states, error states, and success feedback.
- Test on real narrow viewports; do not solve every overflow with horizontal scrolling.

---

## 16. Recommended Frontend Stack

For a Next.js admin app:

| Concern | Recommendation |
|---|---|
| Framework | Next.js |
| Language | TypeScript |
| Styling | Tailwind CSS |
| Accessible primitives | shadcn/ui |
| Data tables | TanStack Table |
| Forms | React Hook Form |
| Validation | Zod |
| Server state | TanStack Query where appropriate |
| Icons | One consistent outline icon set |
| API | Central NestJS REST API |
| Charts | A consistent, responsive chart library |

Keep storefront and admin design systems distinct, while sharing low-level primitives where useful. The backend remains the source of truth for inventory, order state, prices, permissions, and business rules.

### Suggested project structure

```text
src/
├── app/
│   └── admin/
│       ├── layout.tsx
│       ├── dashboard/
│       ├── products/
│       ├── inventory/
│       ├── purchasing/
│       ├── shipments/
│       ├── warehouses/
│       ├── orders/
│       ├── returns/
│       ├── customers/
│       ├── reports/
│       └── settings/
├── components/
│   ├── admin/
│   ├── data-display/
│   ├── forms/
│   └── feedback/
├── features/
│   ├── products/
│   ├── inventory/
│   ├── purchasing/
│   ├── orders/
│   └── customers/
├── lib/
│   ├── api-client.ts
│   ├── permissions.ts
│   ├── validation.ts
│   └── formatting.ts
└── styles/
    ├── globals.css
    └── design-tokens.css
```

This is a suggested organization, not a requirement to create every folder on day one.

---

## 17. Suggested Reusable Components

Build these once and use them throughout the app:

- `AdminShell`, `Sidebar`, `MobileNavigation`, `Topbar`, `Breadcrumbs`
- `PageHeader`, `KpiCard`, `StatusBadge`, `FilterBar`
- `DataTable`, `MobileRecordCard`, `Pagination`
- `FormField`, `SearchSelect`, `ImageUploader`, `QuantityInput`
- `ConfirmationDialog`, `Toast`, `NotificationPanel`
- `LoadingSkeleton`, `EmptyState`, `ErrorState`
- `StockSummary`, `VariantStockTable`, `StockMovementTable`
- `ProductEditor`, `VariantEditor`, `MeasurementEditor`

Shared components should have consistent spacing, focus states, keyboard behavior, loading behavior, and error handling.

---

## 18. Development Roadmap

Build the system in this order:

### Phase 1 — Foundation
- Design tokens and global styles
- Authentication and authorization
- Admin shell and responsive navigation
- Reusable form, table, badge, dialog, and feedback components
- API client and error handling

### Phase 2 — Product Catalogue
- Brands and categories
- Product create/edit/list/detail
- Image upload and ordering
- Variants, SKUs, sizes, colors
- Measurements
- Publication workflow

### Phase 3 — Inventory
- Warehouses
- Variant-level stock overview
- Immutable stock movements
- Stock adjustments
- Low-stock alerts and reorder rules

### Phase 4 — Purchasing and Receiving
- Suppliers
- Purchase orders and approval
- Incoming shipments
- Partial goods receiving
- Receiving discrepancies

### Phase 5 — Storefront Integration
- Product publishing
- Categories and collections
- Homepage sections
- Product availability and pricing API

### Phase 6 — Orders and Fulfillment
- Order list/detail
- Atomic stock reservations
- Payment status
- Dispatch and delivery tracking
- Cancellation handling

### Phase 7 — Returns and Reporting
- Return authorization and inspection
- Refund tracking
- Sales and inventory reports
- Audit logs and notifications

### Phase 8 — Refinement
- Advanced filters and bulk operations
- Reorder recommendations
- Additional approval workflows
- Accessibility and performance audits
- Mobile usability testing

**Build one complete workflow at a time.** For example, finish product creation, variant management, stock receiving, and storefront publication before building advanced analytics.

---

## 19. Definition of Done for Each Screen

Before marking any screen complete, check:

- [ ] Clear page title and primary action
- [ ] Consistent typography, spacing, borders, and colors
- [ ] Responsive layout for desktop, tablet, and mobile
- [ ] Loading, empty, error, and success states
- [ ] Search, filtering, and pagination where required
- [ ] Accessible labels, keyboard navigation, and visible focus
- [ ] Form validation and unsaved-change protection
- [ ] Confirmation for destructive or high-impact actions
- [ ] Correct permission checks in the UI and API
- [ ] Data sourced from the API, not hardcoded production values
- [ ] Server-side validation and transaction safety for business-critical actions
- [ ] Audit trail for important changes

---

## 20. Final Design Principle

Glintora's admin dashboard should feel like a refined, modern SaaS application rather than a cluttered traditional inventory system.

Use a warm ivory background, white surfaces, charcoal typography, restrained sage accents, subtle borders, and purposeful whitespace. Keep navigation predictable, stock quantities transparent, forms organized, and actions obvious.

Above all, the UI must reflect the real business workflow: products move from draft to publication, purchases move through shipment and receiving, inventory changes through recorded movements, and orders progress through explicit fulfillment states.

**The goal:** a new staff member should be able to understand the screen, know what action is available, and complete the task confidently without needing someone to explain the whole system.
