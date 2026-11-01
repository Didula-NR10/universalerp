CREATE TABLE stock_operation (
    idempotency_key uuid PRIMARY KEY,
    operation_type varchar(24) NOT NULL,
    movement_id uuid,
    result_quantity numeric(19,3),
    completed_at timestamptz NOT NULL DEFAULT now()
);
