ALTER TABLE product ADD COLUMN tax_rate numeric(5,2) NOT NULL DEFAULT 0 CHECK (tax_rate >= 0 AND tax_rate <= 100);

CREATE TABLE sale (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branch(id),
    warehouse_id uuid NOT NULL REFERENCES warehouse(id),
    customer_id uuid REFERENCES business_contact(id),
    idempotency_key uuid NOT NULL UNIQUE,
    request_hash char(64) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('COMPLETED','REFUNDED')),
    currency char(3) NOT NULL,
    subtotal numeric(19,2) NOT NULL CHECK (subtotal >= 0),
    discount_total numeric(19,2) NOT NULL CHECK (discount_total >= 0),
    tax_total numeric(19,2) NOT NULL CHECK (tax_total >= 0),
    total numeric(19,2) NOT NULL CHECK (total >= 0),
    created_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    completed_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX sale_branch_time_idx ON sale(branch_id,completed_at DESC);
CREATE INDEX sale_customer_time_idx ON sale(customer_id,completed_at DESC);

CREATE TABLE sale_item (
    id uuid PRIMARY KEY,
    sale_id uuid NOT NULL REFERENCES sale(id),
    product_id uuid NOT NULL REFERENCES product(id),
    product_name varchar(200) NOT NULL,
    quantity numeric(19,3) NOT NULL CHECK (quantity > 0),
    unit_price numeric(19,2) NOT NULL CHECK (unit_price >= 0),
    discount_rate numeric(5,2) NOT NULL CHECK (discount_rate BETWEEN 0 AND 100),
    tax_rate numeric(5,2) NOT NULL CHECK (tax_rate BETWEEN 0 AND 100),
    subtotal numeric(19,2) NOT NULL CHECK (subtotal >= 0),
    discount_amount numeric(19,2) NOT NULL CHECK (discount_amount >= 0),
    tax_amount numeric(19,2) NOT NULL CHECK (tax_amount >= 0),
    total numeric(19,2) NOT NULL CHECK (total >= 0)
);
CREATE INDEX sale_item_sale_idx ON sale_item(sale_id);

CREATE TABLE sale_payment (
    id uuid PRIMARY KEY,
    sale_id uuid NOT NULL REFERENCES sale(id),
    method varchar(24) NOT NULL CHECK (method='CASH'),
    amount numeric(19,2) NOT NULL CHECK (amount >= 0),
    status varchar(16) NOT NULL CHECK (status IN ('RECEIVED','REFUNDED')),
    received_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX sale_payment_cash_uq ON sale_payment(sale_id) WHERE method='CASH';

CREATE TABLE sale_refund (
    id uuid PRIMARY KEY,
    sale_id uuid NOT NULL UNIQUE REFERENCES sale(id),
    idempotency_key uuid NOT NULL UNIQUE,
    amount numeric(19,2) NOT NULL CHECK (amount >= 0),
    refunded_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    refunded_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE sale_refund_item (
    id uuid PRIMARY KEY,
    sale_refund_id uuid NOT NULL REFERENCES sale_refund(id),
    sale_item_id uuid NOT NULL REFERENCES sale_item(id),
    quantity numeric(19,3) NOT NULL CHECK (quantity > 0)
);

INSERT INTO erp_permission(code,description) VALUES
 ('SALE_VIEW','View sales'),('SALE_CREATE','Complete cash sales'),
 ('SALE_DISCOUNT','Apply sale discounts'),('SALE_REFUND','Refund completed cash sales');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE r.code IN ('SUPER_ADMIN','COMPANY_ADMIN')
  AND p.code IN ('SALE_VIEW','SALE_CREATE','SALE_DISCOUNT','SALE_REFUND');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE (r.code='BRANCH_MANAGER' AND p.code IN ('SALE_VIEW','SALE_CREATE','SALE_DISCOUNT','SALE_REFUND'))
   OR (r.code='SALES_MANAGER' AND p.code IN ('SALE_VIEW','SALE_CREATE','SALE_DISCOUNT','SALE_REFUND'))
   OR (r.code='CASHIER' AND p.code IN ('SALE_VIEW','SALE_CREATE'));
