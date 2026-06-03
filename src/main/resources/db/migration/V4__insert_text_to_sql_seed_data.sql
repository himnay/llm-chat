INSERT INTO text2sql_customers (full_name, email, city, segment, created_at) VALUES
    ('Aarav Sharma', 'aarav.sharma@example.com', 'London', 'enterprise', '2025-01-10 09:00:00'),
    ('Olivia Brown', 'olivia.brown@example.com', 'Manchester', 'mid-market', '2025-01-18 10:30:00'),
    ('Noah Wilson', 'noah.wilson@example.com', 'Birmingham', 'small-business', '2025-02-02 14:15:00'),
    ('Emma Taylor', 'emma.taylor@example.com', 'Leeds', 'enterprise', '2025-02-14 11:00:00'),
    ('Liam Johnson', 'liam.johnson@example.com', 'Bristol', 'small-business', '2025-02-20 16:45:00'),
    ('Sophia White', 'sophia.white@example.com', 'London', 'mid-market', '2025-03-05 08:20:00');

INSERT INTO text2sql_products (name, category, unit_price, stock_quantity) VALUES
    ('Cloud Analytics Suite', 'software', 899.00, 120),
    ('AI Chat Assist', 'software', 499.00, 200),
    ('Data Pipeline Pro', 'software', 749.00, 90),
    ('Smart IoT Hub', 'hardware', 299.00, 60),
    ('Edge Sensor Pack', 'hardware', 129.00, 180),
    ('Enterprise Support Plan', 'service', 999.00, 9999);

INSERT INTO text2sql_orders (customer_id, order_date, status, total_amount) VALUES
    (1, '2025-03-10', 'delivered', 1798.00),
    (2, '2025-03-11', 'delivered', 998.00),
    (3, '2025-03-13', 'processing', 1498.00),
    (4, '2025-03-15', 'delivered', 2997.00),
    (5, '2025-03-17', 'cancelled', 299.00),
    (6, '2025-03-20', 'delivered', 1878.00),
    (1, '2025-03-22', 'delivered', 999.00),
    (2, '2025-03-26', 'processing', 1248.00);

INSERT INTO text2sql_order_items (order_id, product_id, quantity, unit_price, discount_pct) VALUES
    (1, 1, 2, 899.00, 0),
    (2, 2, 2, 499.00, 0),
    (3, 3, 2, 749.00, 0),
    (4, 6, 3, 999.00, 0),
    (5, 4, 1, 299.00, 0),
    (6, 2, 1, 499.00, 10),
    (6, 5, 11, 129.00, 2),
    (7, 6, 1, 999.00, 0),
    (8, 3, 1, 749.00, 0),
    (8, 4, 1, 299.00, 0),
    (8, 5, 2, 129.00, 0);
