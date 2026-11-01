CREATE TABLE shift_template (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branch(id),
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    break_minutes integer NOT NULL DEFAULT 0 CHECK (break_minutes BETWEEN 0 AND 480),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    UNIQUE(branch_id,code)
);

CREATE TABLE employee_availability (
    id uuid PRIMARY KEY,
    employee_id uuid NOT NULL REFERENCES employee(id),
    branch_id uuid NOT NULL REFERENCES branch(id),
    weekday smallint NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    start_time time NOT NULL,
    end_time time NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (end_time > start_time),
    UNIQUE(employee_id,weekday,start_time,end_time)
);
CREATE INDEX employee_availability_branch_idx ON employee_availability(branch_id,weekday);

CREATE TABLE work_shift (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branch(id),
    employee_id uuid NOT NULL REFERENCES employee(id),
    template_id uuid REFERENCES shift_template(id),
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','APPROVED','CANCELLED')),
    created_by uuid NOT NULL REFERENCES user_profile(auth_user_id),
    approved_by uuid REFERENCES user_profile(auth_user_id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CHECK (end_at > start_at)
);
CREATE INDEX work_shift_branch_start_idx ON work_shift(branch_id,start_at);
CREATE INDEX work_shift_employee_period_idx ON work_shift(employee_id,start_at,end_at) WHERE status IN ('DRAFT','APPROVED');

INSERT INTO erp_permission(code,description) VALUES
 ('SCHEDULE_VIEW','View branch schedules'),('SCHEDULE_SELF_VIEW','View own shifts and availability'),
 ('SCHEDULE_MANAGE','Create and cancel shifts and templates'),
 ('SCHEDULE_APPROVE','Approve scheduled shifts'),('SCHEDULE_AVAILABILITY','Declare own availability');
INSERT INTO role_permission(role_id,permission_code)
SELECT r.id,p.code FROM erp_role r CROSS JOIN erp_permission p
WHERE (r.code IN ('SUPER_ADMIN','COMPANY_ADMIN') AND p.code LIKE 'SCHEDULE_%')
   OR (r.code IN ('BRANCH_MANAGER','HR_MANAGER') AND p.code IN ('SCHEDULE_VIEW','SCHEDULE_MANAGE','SCHEDULE_APPROVE','SCHEDULE_AVAILABILITY'))
   OR (r.code='EMPLOYEE' AND p.code IN ('SCHEDULE_SELF_VIEW','SCHEDULE_AVAILABILITY'));
