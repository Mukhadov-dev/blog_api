CREATE TABLE subscriptions (
                               id BIGSERIAL PRIMARY KEY,
                               follower_id BIGINT NOT NULL,
                               author_id BIGINT NOT NULL,
                               created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),

                               CONSTRAINT fk_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT unique_follower_author UNIQUE (follower_id, author_id)
);

CREATE INDEX idx_subscriptions_follower ON subscriptions(follower_id);
CREATE INDEX idx_subscriptions_author ON subscriptions(author_id);