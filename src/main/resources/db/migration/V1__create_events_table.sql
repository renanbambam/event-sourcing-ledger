CREATE TABLE account_events (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(36) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB       NOT NULL,
    version      BIGINT      NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_events_aggregate_version UNIQUE (aggregate_id, version)
);

CREATE INDEX idx_events_aggregate_id ON account_events (aggregate_id, version);
