CREATE TABLE local_credential (
    user_id uuid PRIMARY KEY REFERENCES user_profile(auth_user_id) ON DELETE CASCADE,
    password_hash varchar(100) NOT NULL,
    failed_attempts integer NOT NULL DEFAULT 0 CHECK (failed_attempts BETWEEN 0 AND 5),
    locked_until timestamptz,
    session_version bigint NOT NULL DEFAULT 0 CHECK (session_version >= 0),
    must_change_password boolean NOT NULL DEFAULT true,
    password_changed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX local_credential_locked_idx ON local_credential(locked_until) WHERE locked_until IS NOT NULL;
