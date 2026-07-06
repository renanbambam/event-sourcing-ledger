CREATE TABLE account_snapshots (
    aggregate_id VARCHAR(36)   PRIMARY KEY,
    balance      DECIMAL(19,4) NOT NULL,
    currency     VARCHAR(3)    NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    version      BIGINT        NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
