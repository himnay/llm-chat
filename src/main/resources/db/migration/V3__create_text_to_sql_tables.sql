CREATE TABLE text2sql_customers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    city VARCHAR(80) NOT NULL,
    segment VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE text2sql_products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(140) NOT NULL,
    category VARCHAR(80) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE text2sql_orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES text2sql_customers(id),
    order_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL
);

CREATE TABLE text2sql_order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES text2sql_orders(id),
    product_id INTEGER NOT NULL REFERENCES text2sql_products(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    discount_pct NUMERIC(5,2) NOT NULL DEFAULT 0
);
