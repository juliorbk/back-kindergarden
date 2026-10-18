CREATE TABLE events (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(1000),
    location     VARCHAR(255),
    category     VARCHAR(30)   NOT NULL,
    all_day      BOOLEAN       NOT NULL DEFAULT FALSE,
    starts_at    TIMESTAMPTZ   NOT NULL,
    ends_at      TIMESTAMPTZ   NOT NULL,
    status       VARCHAR(30)   NOT NULL,
    published_at TIMESTAMPTZ,
    created_by   BIGINT        NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_events_period CHECK (ends_at >= starts_at)
);

CREATE INDEX idx_events_status_starts_at ON events (status, starts_at);
CREATE INDEX idx_events_range ON events (starts_at, ends_at);
