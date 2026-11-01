CREATE TABLE business_contact (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branch(id),
    contact_type varchar(16) NOT NULL CHECK (contact_type IN ('SUPPLIER','CUSTOMER')),
    name varchar(200) NOT NULL,
    email varchar(320),
    phone varchar(40),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE INDEX business_contact_branch_type_name_idx ON business_contact(branch_id,contact_type,lower(name));

CREATE TABLE expense_category (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(100) NOT NULL UNIQUE,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE expense (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branch(id),
    category_id uuid NOT NULL REFERENCES expense_category(id),
    description varchar(500) NOT NULL,
    amount numeric(19,2) NOT NULL CHECK (amount > 0),
    currency char(3) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','SUBMITTED','APPROVED','REJECTED')),
    created_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    approved_by uuid REFERENCES user_profile(auth_user_id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE INDEX expense_branch_status_idx ON expense(branch_id,status,created_at DESC);

INSERT INTO erp_permission(code,description) VALUES
 ('CONTACT_READ','Read suppliers and customers'),('CONTACT_MANAGE','Manage suppliers and customers'),
 ('EXPENSE_VIEW','Read expenses'),('EXPENSE_CREATE','Create and submit expenses'),
 ('EXPENSE_APPROVE','Approve or reject expenses');
INSERT INTO erp_role(code,description,system_role) VALUES
 ('ACCOUNTANT','Finance operations',true),('HR_MANAGER','Human resources management',true),
 ('SALES_MANAGER','Sales operations',true),('EMPLOYEE','Basic employee access',true);
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE r.code IN ('SUPER_ADMIN','COMPANY_ADMIN')
  AND p.code IN ('CONTACT_READ','CONTACT_MANAGE','EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE (r.code='BRANCH_MANAGER' AND p.code IN ('CONTACT_READ','CONTACT_MANAGE','EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE'))
   OR (r.code='ACCOUNTANT' AND p.code IN ('EXPENSE_VIEW','EXPENSE_CREATE','EXPENSE_APPROVE','REPORT_VIEW'))
   OR (r.code='SALES_MANAGER' AND p.code IN ('CONTACT_READ','CONTACT_MANAGE','PRODUCT_READ'))
   OR (r.code='EMPLOYEE' AND p.code='EXPENSE_CREATE');
