-- Navigation categories: Women / Men as top-level browse entry points.
-- Keeps existing subcategories, re-parented under the right gender category.

INSERT INTO categories (name, parent_category_id) VALUES
  ('Women', NULL),
  ('Men', NULL);

UPDATE categories SET parent_category_id = (SELECT id FROM categories WHERE name = 'Women')
  WHERE name IN ('Embroidered Suit', 'Three Piece');

UPDATE categories SET parent_category_id = (SELECT id FROM categories WHERE name = 'Men')
  WHERE name = 'Panjabi';
