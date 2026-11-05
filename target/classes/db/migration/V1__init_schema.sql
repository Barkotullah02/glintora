-- Glintora Database Schema Initial Migration
-- PostgreSQL compatible

CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    logo_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sizes (
    id BIGSERIAL PRIMARY KEY,
    size_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE colors (
    id BIGSERIAL PRIMARY KEY,
    color_name VARCHAR(50) NOT NULL UNIQUE,
    hex_code VARCHAR(10)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    brand_id BIGINT REFERENCES brands(id),
    category_id BIGINT REFERENCES categories(id),
    description TEXT,
    material VARCHAR(255),
    status VARCHAR(50) DEFAULT 'draft' CHECK (status IN ('draft', 'pending_review', 'published', 'hidden', 'discontinued')),
    default_selling_price NUMERIC(12, 2),
    cost_price NUMERIC(12, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(100) NOT NULL UNIQUE,
    size_id BIGINT REFERENCES sizes(id),
    color_id BIGINT REFERENCES colors(id),
    weight NUMERIC(8, 2), -- in grams
    selling_price NUMERIC(12, 2),
    status VARCHAR(50) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE
);

CREATE TABLE product_attributes (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    attribute_name VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL
);

CREATE TABLE product_measurements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    size_id BIGINT NOT NULL REFERENCES sizes(id),
    garment_section VARCHAR(100) NOT NULL, -- e.g. Shirt, Trouser
    measurement_name VARCHAR(100) NOT NULL, -- e.g. Length, Chest
    measurement_value NUMERIC(8, 2) NOT NULL,
    unit VARCHAR(20) DEFAULT 'inch'
);

CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_details TEXT,
    country VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    location VARCHAR(255)
);

CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id),
    order_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'draft' CHECK (status IN ('draft', 'ordered', 'partial', 'received', 'cancelled')),
    currency VARCHAR(10) DEFAULT 'BDT',
    expected_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_order_items (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    quantity_ordered INT NOT NULL,
    unit_cost NUMERIC(12, 2) NOT NULL
);

CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    shipment_reference VARCHAR(100),
    dispatch_date DATE,
    expected_arrival_date DATE,
    status VARCHAR(50) DEFAULT 'dispatched'
);

CREATE TABLE goods_receipts (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT REFERENCES shipments(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    received_by VARCHAR(100)
);

CREATE TABLE goods_receipt_items (
    id BIGSERIAL PRIMARY KEY,
    goods_receipt_id BIGINT NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    quantity_received INT NOT NULL,
    quantity_rejected INT DEFAULT 0
);

CREATE TABLE inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    movement_type VARCHAR(50) NOT NULL, -- receipt, sale_dispatch, customer_return, supplier_return, damage_writeoff, adjustment_in, adjustment_out, transfer_in, transfer_out
    quantity INT NOT NULL, -- positive for in, negative for out
    reference_type VARCHAR(50), -- goods_receipt, order, adjustment, etc.
    reference_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_balances (
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    quantity_on_hand INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (variant_id, warehouse_id)
);

CREATE TABLE inventory_reorder_rules (
    id BIGSERIAL PRIMARY KEY,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    reorder_point INT NOT NULL DEFAULT 2,
    target_quantity INT NOT NULL DEFAULT 30,
    UNIQUE (variant_id, warehouse_id)
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'ROLE_CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customer_addresses (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    address_type VARCHAR(50) DEFAULT 'delivery', -- delivery, billing
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE
);

CREATE TABLE sales_channels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    sales_channel_id BIGINT NOT NULL REFERENCES sales_channels(id),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'processing', 'shipped', 'delivered', 'cancelled', 'returned')),
    subtotal NUMERIC(12, 2) NOT NULL,
    discount_total NUMERIC(12, 2) DEFAULT 0,
    shipping_fee NUMERIC(12, 2) DEFAULT 0,
    grand_total NUMERIC(12, 2) NOT NULL,
    shipping_address_id BIGINT REFERENCES customer_addresses(id)
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL, -- price snapshot
    unit_cost NUMERIC(12, 2) NOT NULL, -- cost snapshot
    discount_amount NUMERIC(12, 2) DEFAULT 0
);

CREATE TABLE stock_reservations (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT REFERENCES order_items(id) ON DELETE CASCADE,
    variant_id BIGINT NOT NULL REFERENCES product_variants(id),
    warehouse_id BIGINT NOT NULL REFERENCES warehouses(id),
    quantity INT NOT NULL,
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'fulfilled', 'released')),
    expires_at TIMESTAMP
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method VARCHAR(50) NOT NULL, -- COD, bKash, SSLCommerz, Card
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending' CHECK (status IN ('pending', 'paid', 'failed', 'refunded')),
    transaction_reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE delivery_shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    courier VARCHAR(100),
    tracking_number VARCHAR(100),
    status VARCHAR(50) DEFAULT 'preparing',
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP
);

CREATE TABLE product_reviews (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    status VARCHAR(50) DEFAULT 'approved',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_variants_sku ON product_variants(sku);
CREATE INDEX idx_inventory_movements_variant ON inventory_movements(variant_id);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
