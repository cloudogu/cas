CREATE TABLE personal_access_tokens (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    display_name TEXT NOT NULL,
    token_fingerprint BLOB NOT NULL,
    created_at TEXT NOT NULL,
    expires_at TEXT NULL,
    scope TEXT NOT NULL
);

CREATE INDEX idx_personal_access_tokens_user_created
    ON personal_access_tokens (user_id, created_at DESC);

CREATE INDEX idx_personal_access_tokens_fingerprint
    ON personal_access_tokens (token_fingerprint);
