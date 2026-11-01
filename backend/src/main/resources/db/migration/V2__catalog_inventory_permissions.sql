CREATE TABLE product_category (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(120) NOT NULL UNIQUE,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE product (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid REFERENCES product_category(id),
    sku varchar(80) NOT NULL,
    barcode varchar(80),
    name varchar(200) NOT NULL,
    description varchar(1000),
    unit varchar(24) NOT NULL DEFAULT 'EACH',
    price numeric(19,2) NOT NULL CHECK (price >= 0),
    currency char(3) NOT NULL,
    reorder_level numeric(19,3) NOT NULL DEFAULT 0 CHECK (reorder_level >= 0),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE UNIQUE INDEX product_sku_uq ON product(lower(sku));
CREATE UNIQUE INDEX product_barcode_uq ON product(barcode) WHERE barcode IS NOT NULL;
CREATE INDEX product_name_idx ON product(lower(name));
CREATE INDEX product_category_idx ON product(category_id);

CREATE TABLE stock_balance (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id uuid NOT NULL REFERENCES warehouse(id),
    product_id uuid NOT NULL REFERENCES product(id),
    quantity numeric(19,3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    UNIQUE (warehouse_id, product_id)
);
CREATE INDEX stock_balance_product_idx ON stock_balance(product_id);

CREATE TABLE stock_movement (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id uuid NOT NULL REFERENCES warehouse(id),
    product_id uuid NOT NULL REFERENCES product(id),
    quantity_delta numeric(19,3) NOT NULL CHECK (quantity_delta <> 0),
    movement_type varchar(24) NOT NULL CHECK (movement_type IN ('RECEIPT', 'ISSUE', 'ADJUSTMENT', 'TRANSFER_IN', 'TRANSFER_OUT')),
    reference_type varchar(40) NOT NULL,
    reference_id varchar(100) NOT NULL,
    actor_user_id uuid NOT NULL REFERENCES user_profile(auth_user_id),
    occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX stock_movement_warehouse_time_idx ON stock_movement(warehouse_id, occurred_at DESC);
CREATE INDEX stock_movement_product_time_idx ON stock_movement(product_id, occurred_at DESC);
CREATE INDEX stock_movement_reference_idx ON stock_movement(reference_type, reference_id);

INSERT INTO erp_permission(code, description) VALUES
 ('BRANCH_READ','Read branch details'), ('BRANCH_MANAGE','Create and edit branches'),
 ('WAREHOUSE_READ','Read warehouse details'), ('WAREHOUSE_MANAGE','Create and edit warehouses'),
 ('PRODUCT_READ','Read catalog'), ('PRODUCT_CREATE','Create products'), ('PRODUCT_UPDATE','Edit products'),
 ('INVENTORY_VIEW','Read stock balances and movements'), ('INVENTORY_ADJUST','Record receipts, issues and adjustments'),
 ('INVENTORY_TRANSFER','Transfer stock between warehouses'),
 ('USER_MANAGE','Manage ERP users'), ('ROLE_MANAGE','Manage roles and permissions'),
 ('REPORT_VIEW','View business reports'), ('AUDIT_VIEW','View audit history');

INSERT INTO erp_role(code, description, system_role) VALUES
 ('SUPER_ADMIN','Full company administration',true),
 ('COMPANY_ADMIN','Company administration',true),
 ('BRANCH_MANAGER','Branch operations',true),
 ('INVENTORY_MANAGER','Inventory operations',true),
 ('CASHIER','Point of sale',true),
 ('AUDITOR','Read-only audit access',true);

INSERT INTO role_permission(role_id, permission_code)
SELECT r.id, p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE r.code IN ('SUPER_ADMIN','COMPANY_ADMIN');

INSERT INTO role_permission(role_id, permission_code)
SELECT r.id, p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE (r.code = 'BRANCH_MANAGER' AND p.code IN ('BRANCH_READ','WAREHOUSE_READ','PRODUCT_READ','INVENTORY_VIEW','REPORT_VIEW'))
   OR (r.code = 'INVENTORY_MANAGER' AND p.code IN ('WAREHOUSE_READ','PRODUCT_READ','PRODUCT_CREATE','PRODUCT_UPDATE','INVENTORY_VIEW','INVENTORY_ADJUST','INVENTORY_TRANSFER'))
   OR (r.code = 'CASHIER' AND p.code IN ('PRODUCT_READ','INVENTORY_VIEW'))
   OR (r.code = 'AUDITOR' AND p.code IN ('BRANCH_READ','WAREHOUSE_READ','PRODUCT_READ','INVENTORY_VIEW','REPORT_VIEW','AUDIT_VIEW'));
