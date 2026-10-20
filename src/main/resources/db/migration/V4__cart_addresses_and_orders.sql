CREATE TABLE addresses (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  mobile VARCHAR(10) NOT NULL,
  address_line VARCHAR(300) NOT NULL,
  city VARCHAR(100) NOT NULL,
  pincode VARCHAR(6) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_addresses_user (user_id, id)
);

CREATE TABLE carts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE cart_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uk_cart_items_product UNIQUE (cart_id, product_id),
  CONSTRAINT chk_cart_items_quantity CHECK (quantity BETWEEN 1 AND 99)
);

CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PLACED',
  payment_status VARCHAR(30) NOT NULL DEFAULT 'DEMO_UNPAID',
  subtotal_inr DECIMAL(10,2) NOT NULL,
  delivery_fee_inr DECIMAL(10,2) NOT NULL,
  total_inr DECIMAL(10,2) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  mobile VARCHAR(10) NOT NULL,
  address_line VARCHAR(300) NOT NULL,
  city VARCHAR(100) NOT NULL,
  pincode VARCHAR(6) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_orders_totals CHECK (subtotal_inr >= 0 AND delivery_fee_inr >= 0 AND total_inr = subtotal_inr + delivery_fee_inr),
  INDEX idx_orders_user_created (user_id, created_at)
);

CREATE TABLE order_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  unit VARCHAR(40) NOT NULL,
  unit_price_inr DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT chk_order_items_price CHECK (unit_price_inr >= 0),
  CONSTRAINT chk_order_items_quantity CHECK (quantity BETWEEN 1 AND 99)
);
