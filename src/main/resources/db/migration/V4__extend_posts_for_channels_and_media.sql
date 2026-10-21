ALTER TABLE posts ALTER COLUMN caption TYPE VARCHAR(4096);

CREATE TABLE media_assets (
    id          BIGSERIAL PRIMARY KEY,
    storage_key VARCHAR(255) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    mime_type   VARCHAR(100) NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    uploaded_by BIGINT       NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_media_assets_storage_key UNIQUE (storage_key)
);

CREATE TABLE post_media (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    media_id   BIGINT NOT NULL REFERENCES media_assets (id),
    sort_order INT    NOT NULL,
    CONSTRAINT uk_post_media_post_media UNIQUE (post_id, media_id)
);

CREATE INDEX idx_post_media_post_order ON post_media (post_id, sort_order);

CREATE TABLE post_channels (
    id            BIGSERIAL PRIMARY KEY,
    post_id       BIGINT      NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    channel       VARCHAR(20) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_id   VARCHAR(255),
    error_message VARCHAR(500),
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_post_channels_post_channel UNIQUE (post_id, channel)
);

CREATE INDEX idx_post_channels_status ON post_channels (status);
