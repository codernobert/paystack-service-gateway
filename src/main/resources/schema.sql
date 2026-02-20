-- Create Database (run this separately)
-- CREATE DATABASE ecommerce_db;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Products Table
CREATE TABLE IF NOT EXISTS products (
                                        id BIGSERIAL PRIMARY KEY,
                                        name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    sub_category VARCHAR(50),
    brand VARCHAR(100),
    size VARCHAR(20),
    color VARCHAR(50),
    gender VARCHAR(20),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- Shopping Carts Table
CREATE TABLE IF NOT EXISTS shopping_carts (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL,
                                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Cart Items Table
CREATE TABLE IF NOT EXISTS cart_items (
                                          id BIGSERIAL PRIMARY KEY,
                                          cart_id BIGINT NOT NULL,
                                          product_id BIGINT NOT NULL,
                                          quantity INTEGER NOT NULL DEFAULT 1,
                                          price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES shopping_carts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE(cart_id, product_id)
    );

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
                                      id BIGSERIAL PRIMARY KEY,
                                      user_id BIGINT NOT NULL,
                                      order_number VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_intent_id VARCHAR(255),
    shipping_address TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
    );

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
                                           id BIGSERIAL PRIMARY KEY,
                                           order_id BIGINT NOT NULL,
                                           product_id BIGINT NOT NULL,
                                           product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
    );

-- Indexes
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_cart_user ON shopping_carts(user_id);

-- Sample Data
-- INSERT INTO users (email, password_hash, first_name, last_name, role) VALUES
--                                                                           ('admin@clothesshop.com', '$2a$10$dummyhash', 'Admin', 'User', 'ADMIN')
--     ON CONFLICT (email) DO NOTHING;
--

-- INSERT INTO products (name, description, price, category, sub_category, brand, size, color, gender, stock_quantity, image_url)
-- VALUES
--         ('Classic Cotton T-Shirt', 'Comfortable cotton t-shirt for everyday wear', 29.99, 'TOPS', 'T-SHIRTS', 'BasicWear', 'M', 'White', 'UNISEX', 100, 'https://example.com/tshirt1.jpg'),
--         ('Slim Fit Jeans', 'Modern slim fit denim jeans', 79.99, 'BOTTOMS', 'JEANS', 'DenimCo', '32', 'Blue', 'MEN', 50, 'https://example.com/jeans1.jpg'),
--         ('Summer Dress', 'Floral print summer dress', 89.99, 'DRESSES', 'CASUAL', 'FashionFirst', 'S', 'Floral', 'WOMEN', 30, 'https://example.com/dress1.jpg'),
--         ('Leather Jacket', 'Premium leather jacket', 299.99, 'OUTERWEAR', 'JACKETS', 'LuxeStyle', 'L', 'Black', 'UNISEX', 20, 'https://example.com/jacket1.jpg')
--     ON CONFLICT DO NOTHING;
