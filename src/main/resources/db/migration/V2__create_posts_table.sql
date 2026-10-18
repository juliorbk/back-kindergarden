CREATE TABLE posts (
    id           BIGSERIAL PRIMARY KEY,
    slug         VARCHAR(255)  NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    caption      VARCHAR(1000) NOT NULL,
    image_url    VARCHAR(500)  NOT NULL,
    status       VARCHAR(30)   NOT NULL,
    published_at TIMESTAMPTZ,
    author_id    BIGINT        NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_posts_slug UNIQUE (slug)
);

CREATE INDEX idx_posts_status_published_at ON posts (status, published_at DESC);
