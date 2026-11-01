CREATE TABLE company_profile (
    id smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    legal_name varchar(200) NOT NULL,
    display_name varchar(200) NOT NULL,
    base_currency char(3) NOT NULL,
    timezone varchar(80) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE branch (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(32) NOT NULL UNIQUE,
    name varchar(160) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    timezone varchar(80),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE TABLE department (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid REFERENCES branch(id),
    code varchar(32) NOT NULL,
    name varchar(160) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX department_company_code_uq ON department(code) WHERE branch_id IS NULL;
CREATE UNIQUE INDEX department_branch_code_uq ON department(branch_id, code) WHERE branch_id IS NOT NULL;

CREATE TABLE warehouse (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id uuid NOT NULL REFERENCES branch(id),
    code varchar(32) NOT NULL UNIQUE,
    name varchar(160) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX warehouse_branch_idx ON warehouse(branch_id);

CREATE TABLE user_profile (
    auth_user_id uuid PRIMARY KEY,
    email varchar(320) NOT NULL UNIQUE,
    display_name varchar(160) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);

CREATE TABLE erp_role (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(64) NOT NULL UNIQUE,
    description varchar(300) NOT NULL,
    system_role boolean NOT NULL DEFAULT false
);
CREATE TABLE erp_permission (
    code varchar(80) PRIMARY KEY,
    description varchar(300) NOT NULL
);
CREATE TABLE role_permission (
    role_id uuid NOT NULL REFERENCES erp_role(id) ON DELETE CASCADE,
    permission_code varchar(80) NOT NULL REFERENCES erp_permission(code) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_code)
);
CREATE TABLE user_role (
    user_id uuid NOT NULL REFERENCES user_profile(auth_user_id),
    role_id uuid NOT NULL REFERENCES erp_role(id),
    branch_id uuid REFERENCES branch(id),
    assigned_at timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX user_company_role_uq ON user_role(user_id, role_id) WHERE branch_id IS NULL;
CREATE UNIQUE INDEX user_branch_role_uq ON user_role(user_id, role_id, branch_id) WHERE branch_id IS NOT NULL;
CREATE INDEX user_role_branch_idx ON user_role(branch_id, user_id);

CREATE TABLE user_branch (
    user_id uuid NOT NULL REFERENCES user_profile(auth_user_id),
    branch_id uuid NOT NULL REFERENCES branch(id),
    assigned_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, branch_id)
);
CREATE INDEX user_branch_branch_idx ON user_branch(branch_id);

CREATE TABLE audit_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_user_id uuid REFERENCES user_profile(auth_user_id),
    branch_id uuid REFERENCES branch(id),
    action varchar(80) NOT NULL,
    resource_type varchar(80) NOT NULL,
    resource_id varchar(100) NOT NULL,
    outcome varchar(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILURE')),
    summary varchar(500),
    correlation_id varchar(100),
    occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_log_occurred_at_idx ON audit_log(occurred_at DESC);
CREATE INDEX audit_log_branch_occurred_idx ON audit_log(branch_id, occurred_at DESC);
CREATE INDEX audit_log_actor_occurred_idx ON audit_log(actor_user_id, occurred_at DESC);
