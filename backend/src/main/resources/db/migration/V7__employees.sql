CREATE TABLE employee (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branch(id),
    department_id uuid REFERENCES department(id),
    user_id uuid UNIQUE REFERENCES user_profile(auth_user_id),
    employee_number varchar(40) NOT NULL UNIQUE,
    full_name varchar(200) NOT NULL,
    email varchar(320),
    job_position varchar(120) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX employee_branch_name_idx ON employee(branch_id,lower(full_name));

CREATE TABLE attendance (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id uuid NOT NULL REFERENCES employee(id),
    branch_id uuid NOT NULL REFERENCES branch(id),
    work_date date NOT NULL,
    check_in timestamptz NOT NULL,
    check_out timestamptz,
    recorded_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    UNIQUE(employee_id,work_date),
    CHECK (check_out IS NULL OR check_out > check_in)
);
CREATE INDEX attendance_branch_date_idx ON attendance(branch_id,work_date DESC);

CREATE TABLE leave_request (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id uuid NOT NULL REFERENCES employee(id),
    branch_id uuid NOT NULL REFERENCES branch(id),
    start_date date NOT NULL,
    end_date date NOT NULL,
    reason varchar(500) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','APPROVED','REJECTED')),
    requested_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    decided_by uuid REFERENCES user_profile(auth_user_id),
    created_at timestamptz NOT NULL DEFAULT now(),
    decided_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CHECK (end_date >= start_date)
);
CREATE INDEX leave_request_branch_status_idx ON leave_request(branch_id,status,created_at DESC);

INSERT INTO erp_permission(code,description) VALUES
 ('EMPLOYEE_VIEW','View employees'),('EMPLOYEE_MANAGE','Manage employees'),
 ('ATTENDANCE_MANAGE','Record attendance'),('LEAVE_REQUEST','Request leave'),
 ('LEAVE_APPROVE','Approve or reject leave');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE r.code IN ('SUPER_ADMIN','COMPANY_ADMIN')
  AND p.code IN ('EMPLOYEE_VIEW','EMPLOYEE_MANAGE','ATTENDANCE_MANAGE','LEAVE_REQUEST','LEAVE_APPROVE');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE (r.code='HR_MANAGER' AND p.code IN ('EMPLOYEE_VIEW','EMPLOYEE_MANAGE','ATTENDANCE_MANAGE','LEAVE_APPROVE'))
   OR (r.code='BRANCH_MANAGER' AND p.code IN ('EMPLOYEE_VIEW','ATTENDANCE_MANAGE','LEAVE_APPROVE'))
   OR (r.code='EMPLOYEE' AND p.code='LEAVE_REQUEST');
