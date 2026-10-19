CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  slug VARCHAR(90) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  price_inr DECIMAL(10,2) NOT NULL,
  unit VARCHAR(40) NOT NULL,
  stock_quantity INT NOT NULL DEFAULT 0,
  image_url VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT chk_products_price CHECK (price_inr >= 0),
  CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0),
  INDEX idx_products_category (category_id, active),
  FULLTEXT INDEX idx_products_search (name, description)
);

INSERT INTO categories (name, slug) VALUES
 ('Fruit', 'fruit'), ('Vegetables', 'vegetables'), ('Greens', 'greens');

INSERT INTO products (category_id, name, description, price_inr, unit, stock_quantity, image_url) VALUES
 ((SELECT id FROM categories WHERE slug='fruit'), 'Sunshine mangoes', 'Golden, juicy mangoes picked at peak ripeness.', 149.00, '2 pcs', 45, 'https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=900&q=85'),
 ((SELECT id FROM categories WHERE slug='greens'), 'Baby spinach', 'Tender baby spinach from nearby growers.', 49.00, 'bunch', 60, 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?auto=format&fit=crop&w=900&q=85'),
 ((SELECT id FROM categories WHERE slug='vegetables'), 'Heirloom tomatoes', 'Deep red tomatoes bursting with garden-fresh flavour.', 79.00, '500 g', 35, 'https://images.unsplash.com/photo-1546094096-0df4bcaaa337?auto=format&fit=crop&w=900&q=85'),
 ((SELECT id FROM categories WHERE slug='fruit'), 'Avocado duo', 'Creamy avocados for toast, dips, and more.', 129.00, '2 pcs', 25, 'https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&w=900&q=85'),
 ((SELECT id FROM categories WHERE slug='vegetables'), 'Rainbow carrots', 'Sweet, colourful carrots grown in rich soil.', 69.00, '500 g', 40, 'https://images.unsplash.com/photo-1445282768818-728615cc910a?auto=format&fit=crop&w=900&q=85'),
 ((SELECT id FROM categories WHERE slug='fruit'), 'Strawberry crush', 'Small-batch strawberries, sweet and fragrant.', 119.00, '250 g', 22, 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=900&q=85');
