ALTER TABLE warehouse ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0);
ALTER TABLE department ADD COLUMN version bigint NOT NULL DEFAULT 0 CHECK (version >= 0);
INSERT INTO erp_permission(code,description) VALUES
 ('COMPANY_READ','Read company settings'),('COMPANY_MANAGE','Edit company settings'),
 ('DEPARTMENT_READ','Read departments'),('DEPARTMENT_MANAGE','Manage departments');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE r.code IN ('SUPER_ADMIN','COMPANY_ADMIN')
  AND p.code IN ('COMPANY_READ','COMPANY_MANAGE','DEPARTMENT_READ','DEPARTMENT_MANAGE');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE (r.code='BRANCH_MANAGER' AND p.code IN ('DEPARTMENT_READ','DEPARTMENT_MANAGE'))
   OR (r.code='HR_MANAGER' AND p.code IN ('DEPARTMENT_READ'));
