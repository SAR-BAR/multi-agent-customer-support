-- ============================================================
-- V2: Seed data for Phase 2 end-to-end scenarios
-- ============================================================

-- ----------------------------
-- Customers
-- ----------------------------

INSERT INTO customers (id, email, name, created_at) VALUES
    (1, 'priya.sharma@example.com', 'Priya Sharma', '2026-01-15 10:00:00'),
    (2, 'rahul.patel@example.com', 'Rahul Patel', '2026-03-20 14:30:00');

-- ----------------------------
-- Refund policy v1
-- ----------------------------

INSERT INTO refund_policies (id, version, content, effective_from) VALUES
    (1, 1, 'Full refund within 7 days of delivery for wrong or damaged items. Partial refund for late delivery (10% per day late, max 50%). No refund after 30 days. Delivery fee refund for members with free-delivery benefit if fee was wrongly charged. Double-charge: full refund of the duplicate payment. Debited-but-failed: full refund of the debited amount.', '2026-01-01 00:00:00');

-- ----------------------------
-- Approved FAQ answers
-- ----------------------------

INSERT INTO approved_answers (id, general_type, topic, answer, policy_version) VALUES
    (1, 'FAQ', 'return_policy', 'You can return most items within 7 days of delivery. Please visit your order page to start a return.', 1),
    (2, 'FAQ', 'delivery_time', 'Standard delivery takes 3-5 business days. Express delivery takes 1-2 business days.', 1),
    (3, 'FAQ', 'payment_methods', 'We accept UPI, credit cards, debit cards, and net banking.', 1);

-- ----------------------------
-- Orders for Priya (customer 1)
-- ----------------------------

-- Order 1: Late delivery (expected Nov 5, still in transit)
INSERT INTO orders (id, customer_id, status, total_paise, delivery_fee_paise, ordered_at, expected_delivery_at, version) VALUES
    ('ORD-001', 1, 'SHIPPED', 250000, 5000, '2026-10-28 09:00:00', '2026-11-05 23:59:59', 0);

INSERT INTO order_items (order_id, product_name, quantity, unit_price_paise, line_total_paise) VALUES
    ('ORD-001', 'Wireless Headphones', 1, 245000, 245000);

INSERT INTO payments (order_id, status, amount_paise, method, transaction_ref, paid_at) VALUES
    ('ORD-001', 'COMPLETED', 250000, 'UPI', 'TXN-001-A', '2026-10-28 09:05:00');

INSERT INTO shipments (order_id, status, carrier, tracking_number, shipped_at, delivered_at) VALUES
    ('ORD-001', 'IN_TRANSIT', 'BlueDart', 'BD123456789', '2026-10-30 11:00:00', NULL);

INSERT INTO invoices (id, order_id, total_paise, issued_at) VALUES
    (1, 'ORD-001', 250000, '2026-10-28 09:05:00');

INSERT INTO invoice_lines (invoice_id, description, quantity, unit_price_paise, line_total_paise) VALUES
    (1, 'Wireless Headphones', 1, 245000, 245000),
    (1, 'Delivery fee', 1, 5000, 5000);

-- Order 2: Double charge (paid twice for the same order)
INSERT INTO orders (id, customer_id, status, total_paise, delivery_fee_paise, ordered_at, expected_delivery_at, version) VALUES
    ('ORD-002', 1, 'DELIVERED', 180000, 0, '2026-10-20 16:00:00', '2026-10-25 23:59:59', 0);

INSERT INTO order_items (order_id, product_name, quantity, unit_price_paise, line_total_paise) VALUES
    ('ORD-002', 'Cotton Kurta Set', 2, 90000, 180000);

INSERT INTO payments (order_id, status, amount_paise, method, transaction_ref, paid_at) VALUES
    ('ORD-002', 'COMPLETED', 180000, 'CREDIT_CARD', 'TXN-002-A', '2026-10-20 16:05:00'),
    ('ORD-002', 'COMPLETED', 180000, 'CREDIT_CARD', 'TXN-002-B', '2026-10-20 16:06:00');

INSERT INTO shipments (order_id, status, carrier, tracking_number, shipped_at, delivered_at) VALUES
    ('ORD-002', 'DELIVERED', 'Delhivery', 'DL987654321', '2026-10-21 10:00:00', '2026-10-24 14:00:00');

INSERT INTO invoices (id, order_id, total_paise, issued_at) VALUES
    (2, 'ORD-002', 180000, '2026-10-20 16:05:00');

INSERT INTO invoice_lines (invoice_id, description, quantity, unit_price_paise, line_total_paise) VALUES
    (2, 'Cotton Kurta Set', 2, 90000, 180000);

-- ----------------------------
-- Orders for Rahul (customer 2)
-- ----------------------------

-- Order 3: Wrong delivery fee (Rahul has active membership with free delivery)
INSERT INTO orders (id, customer_id, status, total_paise, delivery_fee_paise, ordered_at, expected_delivery_at, version) VALUES
    ('ORD-003', 2, 'DELIVERED', 75000, 5000, '2026-11-01 12:00:00', '2026-11-06 23:59:59', 0);

INSERT INTO order_items (order_id, product_name, quantity, unit_price_paise, line_total_paise) VALUES
    ('ORD-003', 'Phone Case', 1, 70000, 70000);

INSERT INTO payments (order_id, status, amount_paise, method, transaction_ref, paid_at) VALUES
    ('ORD-003', 'COMPLETED', 75000, 'UPI', 'TXN-003-A', '2026-11-01 12:05:00');

INSERT INTO shipments (order_id, status, carrier, tracking_number, shipped_at, delivered_at) VALUES
    ('ORD-003', 'DELIVERED', 'DTDC', 'DT111222333', '2026-11-02 09:00:00', '2026-11-05 11:00:00');

INSERT INTO invoices (id, order_id, total_paise, issued_at) VALUES
    (3, 'ORD-003', 75000, '2026-11-01 12:05:00');

INSERT INTO invoice_lines (invoice_id, description, quantity, unit_price_paise, line_total_paise) VALUES
    (3, 'Phone Case', 1, 70000, 70000),
    (3, 'Delivery fee', 1, 5000, 5000);

-- Rahul's active membership with free delivery
INSERT INTO memberships (customer_id, status, free_delivery, started_at, expires_at) VALUES
    (2, 'ACTIVE', TRUE, '2026-06-01 00:00:00', '2027-06-01 00:00:00');

-- Order 4: Debited but order failed
INSERT INTO orders (id, customer_id, status, total_paise, delivery_fee_paise, ordered_at, expected_delivery_at, version) VALUES
    ('ORD-004', 2, 'FAILED', 320000, 0, '2026-11-10 18:00:00', NULL, 0);

INSERT INTO order_items (order_id, product_name, quantity, unit_price_paise, line_total_paise) VALUES
    ('ORD-004', 'Bluetooth Speaker', 1, 320000, 320000);

INSERT INTO payments (order_id, status, amount_paise, method, transaction_ref, paid_at) VALUES
    ('ORD-004', 'COMPLETED', 320000, 'NET_BANKING', 'TXN-004-A', '2026-11-10 18:05:00');
