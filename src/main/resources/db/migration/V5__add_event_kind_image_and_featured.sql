ALTER TABLE events
    ADD COLUMN kind      VARCHAR(20) NOT NULL DEFAULT 'CALENDAR',
    ADD COLUMN image_url VARCHAR(500),
    ADD COLUMN featured  BOOLEAN     NOT NULL DEFAULT FALSE;

CREATE INDEX idx_events_status_kind_starts_at ON events (status, kind, starts_at);
