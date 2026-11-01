ALTER TABLE sale DROP CONSTRAINT IF EXISTS sale_status_check;
ALTER TABLE sale ADD CONSTRAINT sale_status_check CHECK (status IN ('COMPLETED','PARTIALLY_REFUNDED','REFUNDED'));
ALTER TABLE sale_payment DROP CONSTRAINT IF EXISTS sale_payment_status_check;
ALTER TABLE sale_payment ADD CONSTRAINT sale_payment_status_check CHECK (status IN ('RECEIVED','PARTIALLY_REFUNDED','REFUNDED'));

ALTER TABLE sale_refund DROP CONSTRAINT IF EXISTS sale_refund_sale_id_key;
ALTER TABLE sale_refund ADD COLUMN reason varchar(500) NOT NULL DEFAULT 'Full cash refund';
ALTER TABLE sale_refund ADD COLUMN status varchar(24) NOT NULL DEFAULT 'COMPLETED'
    CHECK (status IN ('PENDING_APPROVAL','COMPLETED','REJECTED'));
ALTER TABLE sale_refund ADD COLUMN request_hash char(64);
ALTER TABLE sale_refund ADD COLUMN approved_by uuid REFERENCES user_profile(auth_user_id);
ALTER TABLE sale_refund ADD COLUMN approved_at timestamptz;
UPDATE sale_refund SET approved_by=refunded_by,approved_at=refunded_at WHERE status='COMPLETED';
CREATE INDEX sale_refund_sale_status_idx ON sale_refund(sale_id,status,refunded_at DESC);

ALTER TABLE sale_refund_item ADD COLUMN amount numeric(19,2) NOT NULL DEFAULT 0 CHECK (amount >= 0);
ALTER TABLE sale_refund_item ADD COLUMN restock boolean NOT NULL DEFAULT true;
UPDATE sale_refund_item r SET amount=si.total FROM sale_item si WHERE si.id=r.sale_item_id;
CREATE UNIQUE INDEX sale_refund_item_once_idx ON sale_refund_item(sale_refund_id,sale_item_id);

INSERT INTO erp_permission(code,description) VALUES ('SALE_REFUND_REQUEST','Request a cash sale refund');
INSERT INTO role_permission(role_id,permission_code)
SELECT id,'SALE_REFUND_REQUEST' FROM erp_role
WHERE code IN ('SUPER_ADMIN','COMPANY_ADMIN','BRANCH_MANAGER','SALES_MANAGER','CASHIER');
