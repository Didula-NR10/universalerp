CREATE TABLE purchase_order (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branch(id),
    supplier_id uuid NOT NULL REFERENCES business_contact(id),
    warehouse_id uuid NOT NULL REFERENCES warehouse(id),
    status varchar(24) NOT NULL CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','PARTIALLY_RECEIVED','RECEIVED','REJECTED','CANCELLED')),
    currency char(3) NOT NULL,
    total numeric(19,2) NOT NULL CHECK (total >= 0),
    created_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    approved_by uuid REFERENCES user_profile(auth_user_id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX purchase_order_branch_status_idx ON purchase_order(branch_id,status,created_at DESC);
CREATE INDEX purchase_order_supplier_idx ON purchase_order(supplier_id,created_at DESC);

CREATE TABLE purchase_order_item (
    id uuid PRIMARY KEY,
    purchase_order_id uuid NOT NULL REFERENCES purchase_order(id),
    product_id uuid NOT NULL REFERENCES product(id),
    ordered_quantity numeric(19,3) NOT NULL CHECK (ordered_quantity > 0),
    received_quantity numeric(19,3) NOT NULL DEFAULT 0 CHECK (received_quantity >= 0),
    unit_cost numeric(19,2) NOT NULL CHECK (unit_cost >= 0),
    CHECK (received_quantity <= ordered_quantity)
);
CREATE INDEX purchase_order_item_order_idx ON purchase_order_item(purchase_order_id);

CREATE TABLE goods_receipt (
    id uuid PRIMARY KEY,
    purchase_order_id uuid NOT NULL REFERENCES purchase_order(id),
    idempotency_key uuid NOT NULL UNIQUE,
    received_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    received_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE goods_receipt_item (
    id uuid PRIMARY KEY,
    goods_receipt_id uuid NOT NULL REFERENCES goods_receipt(id),
    purchase_order_item_id uuid NOT NULL REFERENCES purchase_order_item(id),
    quantity numeric(19,3) NOT NULL CHECK (quantity > 0)
);
CREATE INDEX goods_receipt_item_receipt_idx ON goods_receipt_item(goods_receipt_id);

INSERT INTO erp_permission(code,description) VALUES
 ('PURCHASE_VIEW','View purchase orders'),('PURCHASE_CREATE','Create purchase orders'),
 ('PURCHASE_APPROVE','Approve purchase orders'),('PURCHASE_RECEIVE','Receive ordered goods');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE r.code IN ('SUPER_ADMIN','COMPANY_ADMIN')
  AND p.code IN ('PURCHASE_VIEW','PURCHASE_CREATE','PURCHASE_APPROVE','PURCHASE_RECEIVE');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE (r.code='BRANCH_MANAGER' AND p.code IN ('PURCHASE_VIEW','PURCHASE_CREATE','PURCHASE_APPROVE','PURCHASE_RECEIVE'))
   OR (r.code='INVENTORY_MANAGER' AND p.code IN ('PURCHASE_VIEW','PURCHASE_RECEIVE'));
