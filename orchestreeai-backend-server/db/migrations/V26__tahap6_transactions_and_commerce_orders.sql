-- ==============================================================================
-- OrchestreeAI Migration: Tahap 6 — Transaksi & Siklus Order Commerce
-- File: V26__tahap6_transactions_and_commerce_orders.sql
-- PRD Source: Master Bagian 16, Addendum 1 Bagian 39 (Cart, Quotation, Order, Payment & Logistics)
-- Deskripsi: Skema keranjang belanja, penawaran harga B2B, order multi-channel, transaksi payment gateway (Midtrans/Xendit/Stripe), webhook idempotency log, dan live tracking logistik pengiriman (Biteship/JNE/J&T/SiCepat).
-- ==============================================================================

-- 1. Carts (Keranjang Belanja Konsumen & B2B)
CREATE TABLE IF NOT EXISTS carts (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    conversation_id TEXT,
    channel_account_id TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'CHECKED_OUT', 'ABANDONED', 'EXPIRED'
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    shipping_fee DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    currency TEXT NOT NULL DEFAULT 'IDR',
    notes TEXT NOT NULL DEFAULT '',
    last_activity_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    abandoned_recovery_triggered_at BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_carts_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_carts_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_carts_tenant_id ON carts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_carts_customer_id ON carts(customer_id);
CREATE INDEX IF NOT EXISTS idx_carts_conversation_id ON carts(conversation_id);
CREATE INDEX IF NOT EXISTS idx_carts_status ON carts(status);
CREATE INDEX IF NOT EXISTS idx_carts_last_activity ON carts(last_activity_at);

-- 2. Cart Items
CREATE TABLE IF NOT EXISTS cart_items (
    id TEXT PRIMARY KEY,
    cart_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    product_id TEXT NOT NULL,
    variant_id TEXT,
    product_name TEXT NOT NULL,
    variant_name TEXT,
    sku TEXT NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    subtotal DOUBLE PRECISION NOT NULL,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL,
    attributes_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id)
        REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_tenant_id ON cart_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON cart_items(product_id);

-- 3. Quotations (Surat Penawaran Harga B2B)
CREATE TABLE IF NOT EXISTS quotations (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    conversation_id TEXT,
    quotation_number TEXT UNIQUE NOT NULL,
    valid_until BIGINT NOT NULL,
    subtotal DOUBLE PRECISION NOT NULL,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    shipping_fee DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL,
    status TEXT NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'EXPIRED'
    terms_and_conditions TEXT NOT NULL DEFAULT 'Penawaran berlaku 14 hari kerja. Harga sudah termasuk PPN.',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_quotations_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_quotations_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_quotations_tenant_id ON quotations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_quotations_customer_id ON quotations(customer_id);
CREATE INDEX IF NOT EXISTS idx_quotations_number ON quotations(quotation_number);
CREATE INDEX IF NOT EXISTS idx_quotations_status ON quotations(status);

-- 4. Orders (Pesanan Transaksi Komersial Multi-Channel)
CREATE TABLE IF NOT EXISTS orders (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    customer_id TEXT NOT NULL,
    conversation_id TEXT,
    cart_id TEXT,
    quotation_id TEXT,
    order_number TEXT UNIQUE NOT NULL,
    order_date BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    customer_name TEXT NOT NULL,
    customer_phone TEXT NOT NULL,
    customer_email TEXT,
    shipping_address TEXT NOT NULL DEFAULT '',
    shipping_city TEXT NOT NULL DEFAULT '',
    shipping_province TEXT NOT NULL DEFAULT '',
    shipping_postal_code TEXT NOT NULL DEFAULT '',
    courier_code TEXT NOT NULL DEFAULT 'JNE', -- 'JNE', 'JNT', 'SICEPAT', 'ANTERAJA', 'BITESHIP'
    courier_service TEXT NOT NULL DEFAULT 'REG',
    courier_tracking_number TEXT, -- Waybill / Resi
    subtotal DOUBLE PRECISION NOT NULL,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tax_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    shipping_fee DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL,
    currency TEXT NOT NULL DEFAULT 'IDR',
    status TEXT NOT NULL DEFAULT 'PENDING_PAYMENT', -- 'PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPING_PREP', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'
    payment_gateway TEXT NOT NULL DEFAULT 'MIDTRANS', -- 'MIDTRANS', 'XENDIT', 'STRIPE', 'MANUAL_TRANSFER'
    payment_method TEXT NOT NULL DEFAULT 'QRIS',
    payment_id TEXT,
    payment_url TEXT,
    snap_token TEXT,
    paid_at BIGINT,
    shipped_at BIGINT,
    delivered_at BIGINT,
    notes TEXT NOT NULL DEFAULT '',
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_orders_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES customers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_conversation_id ON orders(conversation_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_tracking_number ON orders(courier_tracking_number);

-- 5. Order Items
CREATE TABLE IF NOT EXISTS order_items (
    id TEXT PRIMARY KEY,
    order_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    product_id TEXT NOT NULL,
    variant_id TEXT,
    product_name TEXT NOT NULL,
    variant_name TEXT,
    sku TEXT NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    subtotal DOUBLE PRECISION NOT NULL,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_tenant_id ON order_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- 6. Payment Transactions (Catatan Transaksi Payment Gateway Midtrans/Xendit/Stripe)
CREATE TABLE IF NOT EXISTS payment_transactions (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    order_id TEXT NOT NULL,
    invoice_id TEXT,
    gateway_type TEXT NOT NULL DEFAULT 'MIDTRANS', -- 'MIDTRANS', 'XENDIT', 'STRIPE'
    transaction_id TEXT NOT NULL,
    external_reference TEXT NOT NULL,
    payment_type TEXT NOT NULL DEFAULT 'QRIS',
    gross_amount DOUBLE PRECISION NOT NULL,
    currency TEXT NOT NULL DEFAULT 'IDR',
    transaction_status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'SETTLEMENT', 'CAPTURE', 'DENY', 'CANCEL', 'EXPIRE', 'REFUND'
    settlement_time BIGINT,
    signature_hash TEXT,
    raw_response_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_payment_trans_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_trans_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payment_trans_tenant_id ON payment_transactions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_trans_order_id ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_trans_tx_id ON payment_transactions(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payment_trans_status ON payment_transactions(transaction_status);

-- 7. Payment Webhooks Log (Idempotency & Audit Trail Callback Gateway)
CREATE TABLE IF NOT EXISTS payment_webhooks_log (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    gateway_type TEXT NOT NULL, -- 'MIDTRANS', 'XENDIT'
    event_type TEXT NOT NULL,
    order_id TEXT NOT NULL,
    raw_payload TEXT NOT NULL,
    signature_header TEXT NOT NULL,
    is_signature_valid BOOLEAN NOT NULL DEFAULT TRUE,
    http_status_code INT NOT NULL DEFAULT 200,
    process_status TEXT NOT NULL DEFAULT 'PROCESSED', -- 'PROCESSED', 'REJECTED_INVALID_SIGNATURE', 'ERROR'
    error_message TEXT,
    received_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    processed_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_payment_webhooks_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payment_webhooks_tenant_id ON payment_webhooks_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payment_webhooks_order_id ON payment_webhooks_log(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_webhooks_status ON payment_webhooks_log(process_status);

-- 8. Shipments (Pemesanan & Ekspedisi Logistik)
CREATE TABLE IF NOT EXISTS shipments (
    id TEXT PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    order_id TEXT NOT NULL,
    courier_code TEXT NOT NULL, -- 'JNE', 'JNT', 'SICEPAT', 'ANTERAJA', 'BITESHIP'
    courier_service_name TEXT NOT NULL, -- 'REG', 'OKE', 'YES', 'EZ', dll.
    waybill_number TEXT UNIQUE NOT NULL, -- Nomor Resi Pengiriman
    origin_address TEXT NOT NULL,
    destination_address TEXT NOT NULL,
    destination_city TEXT NOT NULL,
    destination_postal_code TEXT NOT NULL,
    weight_kg DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    shipping_fee DOUBLE PRECISION NOT NULL,
    insurance_fee DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status TEXT NOT NULL DEFAULT 'BOOKED', -- 'BOOKED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNED', 'CANCELLED'
    eta_days TEXT NOT NULL DEFAULT '2-3 Hari',
    booked_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    picked_up_at BIGINT,
    delivered_at BIGINT,
    last_tracking_update_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_shipments_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_shipments_tenant_id ON shipments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_shipments_order_id ON shipments(order_id);
CREATE INDEX IF NOT EXISTS idx_shipments_waybill ON shipments(waybill_number);
CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipments(status);

-- 9. Shipment Tracking Events (Audit Trail Perjalanan Ekspedisi Real-Time)
CREATE TABLE IF NOT EXISTS shipment_tracking_events (
    id TEXT PRIMARY KEY,
    shipment_id TEXT NOT NULL,
    tenant_id TEXT NOT NULL,
    event_time BIGINT NOT NULL,
    location TEXT NOT NULL,
    courier_status_raw TEXT NOT NULL,
    normalized_status TEXT NOT NULL, -- 'BOOKED', 'PICKED_UP', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'RETURNED', 'EXCEPTION'
    description TEXT NOT NULL,
    driver_name TEXT,
    driver_phone TEXT,
    raw_payload_json TEXT NOT NULL DEFAULT '{}',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    CONSTRAINT fk_tracking_events_shipment FOREIGN KEY (shipment_id)
        REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_tracking_events_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_tracking_events_shipment_id ON shipment_tracking_events(shipment_id);
CREATE INDEX IF NOT EXISTS idx_tracking_events_tenant_id ON shipment_tracking_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tracking_events_status ON shipment_tracking_events(normalized_status);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================
ALTER TABLE carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE quotations ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_webhooks_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE shipments ENABLE ROW LEVEL SECURITY;
ALTER TABLE shipment_tracking_events ENABLE ROW LEVEL SECURITY;

-- 1. Policies for carts
DROP POLICY IF EXISTS tenant_isolation_carts ON carts;
CREATE POLICY tenant_isolation_carts ON carts FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 2. Policies for cart_items
DROP POLICY IF EXISTS tenant_isolation_cart_items ON cart_items;
CREATE POLICY tenant_isolation_cart_items ON cart_items FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 3. Policies for quotations
DROP POLICY IF EXISTS tenant_isolation_quotations ON quotations;
CREATE POLICY tenant_isolation_quotations ON quotations FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 4. Policies for orders
DROP POLICY IF EXISTS tenant_isolation_orders ON orders;
CREATE POLICY tenant_isolation_orders ON orders FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 5. Policies for order_items
DROP POLICY IF EXISTS tenant_isolation_order_items ON order_items;
CREATE POLICY tenant_isolation_order_items ON order_items FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 6. Policies for payment_transactions
DROP POLICY IF EXISTS tenant_isolation_payment_trans ON payment_transactions;
CREATE POLICY tenant_isolation_payment_trans ON payment_transactions FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 7. Policies for payment_webhooks_log
DROP POLICY IF EXISTS tenant_isolation_payment_webhooks ON payment_webhooks_log;
CREATE POLICY tenant_isolation_payment_webhooks ON payment_webhooks_log FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 8. Policies for shipments
DROP POLICY IF EXISTS tenant_isolation_shipments ON shipments;
CREATE POLICY tenant_isolation_shipments ON shipments FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');

-- 9. Policies for shipment_tracking_events
DROP POLICY IF EXISTS tenant_isolation_shipment_tracking ON shipment_tracking_events;
CREATE POLICY tenant_isolation_shipment_tracking ON shipment_tracking_events FOR ALL USING (app_has_tenant_access(tenant_id) OR auth.role() = 'service_role');
