-- Seed data for Glintora
-- Reference data, one warehouse, sample catalogue and opening stock.
-- Opening stock is recorded as receipt movements + balances, mirroring how
-- physical receipts work, so the audit trail starts on day one.

-- ------------------------------------------------------------- reference

INSERT INTO sizes (size_name) VALUES
  ('XS'), ('S'), ('M'), ('L'), ('XL'), ('XXL');

INSERT INTO colors (color_name, hex_code) VALUES
  ('Black', '#1C1C1C'),
  ('Navy', '#1B2A4A'),
  ('Maroon', '#6E1430'),
  ('Olive', '#4B5320'),
  ('White', '#F5F5F5'),
  ('Golden', '#C9A227');

INSERT INTO sales_channels (name) VALUES
  ('Website'), ('Facebook'), ('Instagram'), ('WhatsApp'), ('Retail');

INSERT INTO warehouses (name, location) VALUES
  ('Main Warehouse', 'Dhaka');

-- ------------------------------------------------------------- catalogue

INSERT INTO brands (name, logo_url) VALUES
  ('ETHNC', NULL),
  ('Glintora', NULL);

INSERT INTO categories (name, parent_category_id) VALUES
  ('Ethnic Wear', NULL),
  ('Western Wear', NULL),
  ('Accessories', NULL);

INSERT INTO categories (name, parent_category_id) VALUES
  ('Embroidered Suit', 1),
  ('Panjabi', 1),
  ('Three Piece', 1);

-- Products (all start as draft; publish from the admin dashboard)
INSERT INTO products (product_code, name, brand_id, category_id, description, material, status, default_selling_price, cost_price) VALUES
  ('GLI-001', 'ETHNC Embroidered Suit', 1, 4,
   'Elegant cotton-blend embroidered suit with fine thread work. Two-piece shirt and trouser with dupatta option.',
   'Cotton blend', 'published', 2450.00, 1350.00),
  ('GLI-002', 'Glintora Classic Panjabi', 2, 5,
   'A timeless panjabi cut from premium cotton with subtle self-pattern and a clean silhouette.',
   'Premium cotton', 'draft', 1850.00, 980.00),
  ('GLI-003', 'ETHNC Three Piece Ensemble', 1, 6,
   'Complete three-piece ensemble with shirt, trouser and embroidered dupatta for festive occasions.',
   'Silk blend', 'draft', 3950.00, 2200.00);

-- Variants (size-based; GLI-001 also has colors)
INSERT INTO product_variants (product_id, sku, size_id, color_id, weight, selling_price, status) VALUES
  (1, 'GLI-001-S-BLK',  2, 1, 650, NULL, 'active'),
  (1, 'GLI-001-M-BLK',  3, 1, 700, NULL, 'active'),
  (1, 'GLI-001-L-BLK',  4, 1, 750, NULL, 'active'),
  (1, 'GLI-001-M-NVY',  3, 2, 700, 2550.00, 'active'),
  (2, 'GLI-002-S',      2, NULL, 500, NULL, 'active'),
  (2, 'GLI-002-M',      3, NULL, 550, NULL, 'active'),
  (2, 'GLI-002-L',      4, NULL, 600, NULL, 'active'),
  (2, 'GLI-002-XL',     5, NULL, 620, NULL, 'active'),
  (3, 'GLI-003-M-MRN',  3, 3, 900, NULL, 'active'),
  (3, 'GLI-003-L-MRN',  4, 3, 950, NULL, 'active'),
  (3, 'GLI-003-M-GLD',  3, 6, 900, 4100.00, 'active');

-- Images (local placeholders; replace with real product photography)
INSERT INTO product_images (product_id, image_url, sort_order, is_primary) VALUES
  (1, '/images/products/suit-a.svg', 0, TRUE),
  (1, '/images/products/suit-b.svg', 1, FALSE),
  (2, '/images/products/panjabi-a.svg', 0, TRUE),
  (3, '/images/products/three-piece-a.svg', 0, TRUE);

-- Attributes
INSERT INTO product_attributes (product_id, attribute_name, attribute_value) VALUES
  (1, 'Fabric', 'Cotton blend'), (1, 'Care', 'Hand wash cold'), (1, 'Fit', 'Regular'),
  (2, 'Fabric', 'Premium cotton'), (2, 'Care', 'Machine wash cold'), (2, 'Fit', 'Slim'),
  (3, 'Fabric', 'Silk blend'), (3, 'Care', 'Dry clean only'), (3, 'Fit', 'Regular');

-- Size measurements (shirt section, inches)
INSERT INTO product_measurements (product_id, size_id, garment_section, measurement_name, measurement_value, unit) VALUES
  (1, 2, 'Shirt', 'Length', 37, 'inch'),
  (1, 3, 'Shirt', 'Length', 38, 'inch'),
  (1, 4, 'Shirt', 'Length', 39, 'inch'),
  (1, 2, 'Shirt', 'Body',  41, 'inch'),
  (1, 3, 'Shirt', 'Body',  43, 'inch'),
  (1, 4, 'Shirt', 'Body',  45, 'inch'),
  (1, 3, 'Trouser', 'Waist', 32, 'inch'),
  (1, 4, 'Trouser', 'Waist', 34, 'inch'),
  (2, 2, 'Shirt', 'Length', 44, 'inch'),
  (2, 3, 'Shirt', 'Length', 45, 'inch'),
  (2, 4, 'Shirt', 'Length', 46, 'inch'),
  (3, 3, 'Shirt', 'Length', 42, 'inch'),
  (3, 4, 'Shirt', 'Length', 43, 'inch');

-- --------------------------------------------------------- opening stock
-- Recorded as receipt movements into the Main Warehouse plus balances.

INSERT INTO inventory_movements (variant_id, warehouse_id, movement_type, quantity, reference_type, reference_id, created_at) VALUES
  (1, 1, 'receipt',  5, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (2, 1, 'receipt',  8, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (3, 1, 'receipt',  4, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (4, 1, 'receipt',  6, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (5, 1, 'receipt', 10, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (6, 1, 'receipt', 12, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (7, 1, 'receipt',  9, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (8, 1, 'receipt',  5, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (9, 1, 'receipt',  6, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (10, 1, 'receipt', 4, 'opening_stock', NULL, CURRENT_TIMESTAMP),
  (11, 1, 'receipt', 3, 'opening_stock', NULL, CURRENT_TIMESTAMP);

INSERT INTO inventory_balances (variant_id, warehouse_id, quantity_on_hand, updated_at) VALUES
  (1, 1,  5, CURRENT_TIMESTAMP),
  (2, 1,  8, CURRENT_TIMESTAMP),
  (3, 1,  4, CURRENT_TIMESTAMP),
  (4, 1,  6, CURRENT_TIMESTAMP),
  (5, 1, 10, CURRENT_TIMESTAMP),
  (6, 1, 12, CURRENT_TIMESTAMP),
  (7, 1,  9, CURRENT_TIMESTAMP),
  (8, 1,  5, CURRENT_TIMESTAMP),
  (9, 1,  6, CURRENT_TIMESTAMP),
  (10, 1, 4, CURRENT_TIMESTAMP),
  (11, 1, 3, CURRENT_TIMESTAMP);

-- Reorder rules for a few representative variants
INSERT INTO inventory_reorder_rules (variant_id, warehouse_id, reorder_point, target_quantity) VALUES
  (2, 1, 3, 30),
  (3, 1, 3, 25),
  (6, 1, 4, 40),
  (9, 1, 2, 20),
  (11, 1, 2, 15);
