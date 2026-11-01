CREATE TABLE notification (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES user_profile(auth_user_id),
    branch_id uuid REFERENCES branch(id),
    event_key varchar(180) NOT NULL,
    notification_type varchar(32) NOT NULL,
    title varchar(160) NOT NULL,
    message varchar(500) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    read_at timestamptz,
    UNIQUE(user_id,event_key)
);
CREATE INDEX notification_user_unread_idx ON notification(user_id,created_at DESC) WHERE read_at IS NULL;
CREATE TABLE notification_preference (
    user_id uuid NOT NULL REFERENCES user_profile(auth_user_id),
    notification_type varchar(32) NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    PRIMARY KEY(user_id,notification_type)
);
