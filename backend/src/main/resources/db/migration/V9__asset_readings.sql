CREATE TABLE asset_reading (
 id uuid PRIMARY KEY,
 version bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL,
 updated_at timestamptz NOT NULL,
 created_by uuid REFERENCES app_user(id),
 archived boolean NOT NULL DEFAULT false,
 asset_id uuid NOT NULL REFERENCES asset(id),
 user_id uuid NOT NULL REFERENCES app_user(id),
 work_order_id uuid REFERENCES work_order(id),
 metric varchar(120) NOT NULL,
 reading_value numeric(24,6) NOT NULL,
 unit varchar(40) NOT NULL,
 recorded_at timestamptz NOT NULL,
 note varchar(1000)
);
CREATE INDEX idx_asset_reading_asset_date ON asset_reading(asset_id, recorded_at DESC);
